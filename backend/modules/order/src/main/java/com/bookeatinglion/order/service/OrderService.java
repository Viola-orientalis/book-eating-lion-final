package com.bookeatinglion.order.service;

import com.bookeatinglion.order.cart.repository.CartItemRepository;
import com.bookeatinglion.order.coupon.service.CouponService;
import com.bookeatinglion.order.delivery.domain.Delivery;
import com.bookeatinglion.order.delivery.repository.DeliveryRepository;
import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderItem;
import com.bookeatinglion.order.dto.OrderCreateRequest;
import com.bookeatinglion.order.dto.OrderItemRequest;
import com.bookeatinglion.order.dto.OrderItemResponse;
import com.bookeatinglion.order.dto.OrderResponse;
import com.bookeatinglion.order.dto.OrderSummaryResponse;
import com.bookeatinglion.order.exception.EmptyOrderItemsException;
import com.bookeatinglion.order.exception.LockAcquisitionException;
import com.bookeatinglion.order.exception.OrderNotFoundException;
import com.bookeatinglion.order.exception.UnauthorizedOrderAccessException;
import com.bookeatinglion.order.inventory.domain.InsufficientStockException;
import com.bookeatinglion.order.inventory.domain.Inventory;
import com.bookeatinglion.order.inventory.repository.InventoryRepository;
import com.bookeatinglion.order.repository.OrderItemRepository;
import com.bookeatinglion.order.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 주문 생성의 핵심 트랜잭션. 3중 재고 방어선(Redlock → 낙관적 락 → CHECK 제약) 중
 * 1차 방어선을 여기서 명시적으로 구현한다.
 *
 * 락 획득은 트랜잭션 밖에서 하고, 트랜잭션 커밋/롤백까지 끝난 뒤 락을 해제한다
 * (finally). {@code @Transactional} 을 메서드에 붙이는 대신 {@link TransactionTemplate}
 * 을 쓰는 이유는, 같은 클래스 안에서 자기 자신의 {@code @Transactional} 메서드를 호출하면
 * 프록시를 거치지 않아 트랜잭션이 걸리지 않는 자기호출 함정을 피하기 위해서다.
 */
@Service
public class OrderService {

    private static final long LOCK_WAIT_SECONDS = 5;
    private static final long LOCK_LEASE_SECONDS = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final CouponService couponService;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            InventoryRepository inventoryRepository,
            CartItemRepository cartItemRepository,
            DeliveryRepository deliveryRepository,
            CouponService couponService,
            RedissonClient redissonClient,
            PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.cartItemRepository = cartItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.couponService = couponService;
        this.redissonClient = redissonClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new EmptyOrderItemsException();
        }

        List<Long> sortedBookIds = request.items().stream()
                .map(OrderItemRequest::bookId)
                .distinct()
                .sorted()
                .toList();

        // 항상 bookId 오름차순으로 잠근다 — 락 순서를 고정하지 않으면 두 주문이 서로 다른
        // 순서로 같은 두 책을 잠그다가 데드락에 빠질 수 있다.
        RLock[] locks = sortedBookIds.stream()
                .map(bookId -> redissonClient.getLock(lockKey(bookId)))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean locked;
        try {
            locked = multiLock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(sortedBookIds);
        }
        if (!locked) {
            throw new LockAcquisitionException(sortedBookIds);
        }

        try {
            return transactionTemplate.execute(status -> doCreateOrder(memberId, request));
        } finally {
            multiLock.unlock();
        }
    }

    private OrderResponse doCreateOrder(Long memberId, OrderCreateRequest request) {
        long itemsSubtotal = 0;
        for (OrderItemRequest item : request.items()) {
            // 2차 방어선(@Version 낙관적 락)은 Inventory 엔티티에, 3차 방어선(CHECK stock>=0)은
            // 마이그레이션에 이미 있다. 여기서는 1차 방어선(Redlock) 보호 아래 안전하게 차감한다.
            Inventory inventory = inventoryRepository
                    .findById(item.bookId())
                    .orElseThrow(() -> new InsufficientStockException(item.bookId(), 0, item.quantity()));
            inventory.deduct(item.quantity());
            itemsSubtotal += (long) item.unitPrice() * item.quantity();
        }

        long couponDiscount = 0;
        if (request.memberCouponId() != null) {
            couponDiscount = couponService.validateAndUse(memberId, request.memberCouponId(), itemsSubtotal);
        }

        Order order = Order.create(
                memberId,
                request.addressId(),
                request.memberCouponId(),
                request.recipientName(),
                request.recipientPhone(),
                request.postalCode(),
                request.address(),
                request.addressDetail(),
                itemsSubtotal,
                couponDiscount,
                request.usedPointOrZero());
        orderRepository.save(order);

        List<OrderItem> orderItems = request.items().stream()
                .map(item -> new OrderItem(
                        order.getId(), item.bookId(), item.bookTitle(), item.quantity(), item.unitPrice()))
                .toList();
        orderItemRepository.saveAll(orderItems);

        // 주문 1건에 배송 1건 — uk_deliveries_order 제약과 일치한다.
        deliveryRepository.save(Delivery.builder().orderId(order.getId()).build());

        if (request.cartItemIds() != null && !request.cartItemIds().isEmpty()) {
            cartItemRepository.deleteByIdInAndMemberId(request.cartItemIds(), memberId);
        }

        List<OrderItemResponse> itemResponses =
                orderItems.stream().map(OrderItemResponse::from).toList();
        return OrderResponse.of(order, itemResponses);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long memberId, Long orderId) {
        Order order = requireOwnedOrder(memberId, orderId);
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(orderId).stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.of(order, items);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long memberId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        List<Long> orderIds = orders.map(Order::getId).toList();
        Map<Long, Long> itemCounts = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.counting()));
        return orders.map(order -> OrderSummaryResponse.of(
                order, itemCounts.getOrDefault(order.getId(), 0L).intValue()));
    }

    private Order requireOwnedOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException(orderId);
        }
        return order;
    }

    private String lockKey(Long bookId) {
        return "book:" + bookId;
    }
}

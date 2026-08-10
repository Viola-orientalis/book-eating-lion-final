package com.bookeatinglion.order.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookeatinglion.order.OrderModuleTestApplication;
import com.bookeatinglion.order.inventory.domain.InsufficientStockException;
import com.bookeatinglion.order.inventory.domain.Inventory;
import com.bookeatinglion.order.inventory.repository.InventoryRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 재고 동시성(2차 방어선: @Version 낙관적 락)을 H2가 아니라 실제 Postgres 위에서 검증한다.
 *
 * H2로는 재현할 수 없는 것: 여러 개의 실제 JDBC 커넥션이 동시에 같은 row 를 두고 경합할 때
 * 벌어지는 진짜 MVCC 충돌. 이 테스트는 재고 20권에 30개의 동시 요청을 걸어, 정확히 20건만
 * 성공하고(오버셀링 0건) 나머지는 재고부족으로 실패하는지 확인한다.
 *
 * Redlock(1차 방어선)은 여기서 검증하지 않는다 — Redis 까지 띄우면 모듈 테스트가 아니라
 * apps:order-api 통합 테스트 영역이 된다. Redlock 호출 배선 자체는 OrderServiceTest(Mockito)가
 * 커버한다.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = OrderModuleTestApplication.class)
class InventoryConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 동시_요청_30건_중_재고_20건만_정확히_성공한다() throws InterruptedException {
        inventoryRepository.save(new Inventory(1L, 20));

        int attempts = 30;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(attempts);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    deductWithRetry(transactionTemplate, 1L, 1);
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failed.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(succeeded.get()).isEqualTo(20);
        assertThat(failed.get()).isEqualTo(10);
        Inventory result = inventoryRepository.findById(1L).orElseThrow();
        assertThat(result.getStock()).isEqualTo(0);
    }

    /** 낙관적 락 충돌(다른 스레드가 먼저 커밋)이면 재시도한다 — Redlock 이 있었다면 필요 없는 재시도다. */
    private void deductWithRetry(TransactionTemplate transactionTemplate, Long bookId, int quantity) {
        while (true) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Inventory inventory = inventoryRepository.findById(bookId).orElseThrow();
                    inventory.deduct(quantity);
                });
                return;
            } catch (ObjectOptimisticLockingFailureException retry) {
                // 경합 상대가 먼저 커밋했다 — 최신 버전으로 다시 시도한다.
            }
        }
    }
}

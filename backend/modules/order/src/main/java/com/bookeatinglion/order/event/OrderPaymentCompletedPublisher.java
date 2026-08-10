package com.bookeatinglion.order.event;

import com.bookeatinglion.common.event.OrderPaymentCompleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 결제 승인 완료를 catalog/ai-service 에 비동기로 알린다. {@link ReviewPermissionPublisher} 와
 * 완전히 같은 패턴이다 — Redis Streams 는 기술 스택 문서 §3 에 "MSA 간 비동기 이벤트 대기열"로
 * 이미 명시돼 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentCompletedPublisher {

    private final StringRedisTemplate redisTemplate;

    public void publish(OrderPaymentCompleted event) {
        redisTemplate
                .opsForStream()
                .add(StreamRecords.mapBacked(event.toMap()).withStreamKey(OrderPaymentCompleted.STREAM_KEY));

        log.info("OrderPaymentCompleted 발행: orderId={}, memberId={}", event.orderId(), event.memberId());
    }
}

package com.bookeatinglion.common.event;

import java.util.Map;

/**
 * order-service → catalog-service/ai-service. 결제 승인 완료를 알린다.
 *
 * common 에 두는 이유는 {@link ReviewPermissionGranted} 와 동일 — 발행측(order)과 소비측이
 * modules 경계를 넘어야 하는데 modules 끼리는 서로 의존할 수 없어서다(§7.2). 도메인 코드가
 * 아니라 계약이다.
 *
 * bookIds/quantities 는 결제 확정 시점의 스냅샷이다. 소비측이 굳이 order-service 를 다시
 * 조회하지 않고 이 이벤트만으로 처리할 수 있게 한다.
 */
public record OrderPaymentCompleted(
        Long orderId, Long memberId, long totalAmount, String bookIdsCsv, String paidAt) {

    public static final String STREAM_KEY = "event:order:pay-completed";

    public Map<String, String> toMap() {
        return Map.of(
                "orderId", String.valueOf(orderId),
                "memberId", String.valueOf(memberId),
                "totalAmount", String.valueOf(totalAmount),
                "bookIds", bookIdsCsv == null ? "" : bookIdsCsv,
                "paidAt", paidAt);
    }

    public static OrderPaymentCompleted fromMap(Map<String, String> map) {
        return new OrderPaymentCompleted(
                Long.valueOf(str(map, "orderId")),
                Long.valueOf(str(map, "memberId")),
                Long.parseLong(str(map, "totalAmount")),
                str(map, "bookIds"),
                str(map, "paidAt"));
    }

    private static String str(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "" : value;
    }
}

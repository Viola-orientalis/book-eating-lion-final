package com.bookeatinglion.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 도서 제목/단가를 클라이언트가 실어 보낸다 — order-service 는 catalog_db 에 접근 권한이 없고
 * (README 판단 ② "도서 정보 스냅샷: 통신 없음"), 프론트가 이미 상품 상세에서 조회한 값을
 * 그대로 스냅샷으로 받는다. orders.recipient_name 등 배송지 칼럼과 같은 신뢰 경계다.
 */
public record OrderItemRequest(
        @NotNull Long bookId, @NotBlank String bookTitle, @Min(1) int quantity, @Min(0) long unitPrice) {}

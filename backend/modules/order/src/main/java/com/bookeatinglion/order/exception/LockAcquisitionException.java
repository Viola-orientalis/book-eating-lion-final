package com.bookeatinglion.order.exception;

/** Redlock(1차 방어선) 획득 타임아웃. 재고 경합이 심할 때 발생하며 재시도 대상이다. */
public class LockAcquisitionException extends OrderDomainException {

    public LockAcquisitionException(java.util.List<Long> bookIds) {
        super(OrderErrorCode.LOCK_ACQUISITION_FAILED, "재고 락 획득에 실패했습니다. 잠시 후 다시 시도하세요: bookIds=" + bookIds);
    }
}

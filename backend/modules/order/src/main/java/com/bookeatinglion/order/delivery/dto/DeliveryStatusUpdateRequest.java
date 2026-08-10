package com.bookeatinglion.order.delivery.dto;

import com.bookeatinglion.order.delivery.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

/** courierCompany/trackingNumber 는 SHIPPED 로 전이할 때만 필요하다. */
public record DeliveryStatusUpdateRequest(
        @NotNull DeliveryStatus targetStatus, String courierCompany, String trackingNumber) {}

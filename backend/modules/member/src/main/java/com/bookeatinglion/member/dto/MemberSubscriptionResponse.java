package com.bookeatinglion.member.dto;

import com.bookeatinglion.member.domain.Subscription;
import com.bookeatinglion.member.domain.SubscriptionStatus;

import java.time.LocalDate;

public record MemberSubscriptionResponse(
        boolean subscribed,
        String planName,
        Long monthlyPrice,
        LocalDate nextDeliveryDate
) {
    private static final MemberSubscriptionResponse NOT_SUBSCRIBED =
            new MemberSubscriptionResponse(false, null, null, null);

    public static MemberSubscriptionResponse from(Subscription subscription) {
        if (subscription == null) {
            return NOT_SUBSCRIBED;
        }
        return new MemberSubscriptionResponse(
                subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE,
                subscription.getPlanName(),
                subscription.getMonthlyPrice(),
                subscription.getNextDeliveryDate()
        );
    }
}

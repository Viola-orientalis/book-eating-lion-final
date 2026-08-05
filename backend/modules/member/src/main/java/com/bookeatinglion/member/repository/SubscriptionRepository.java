package com.bookeatinglion.member.repository;

import com.bookeatinglion.member.domain.Subscription;
import com.bookeatinglion.member.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    long countBySubscriptionStatus(SubscriptionStatus subscriptionStatus);

    Optional<Subscription> findFirstByMemberIdOrderByCreatedAtDesc(Long memberId);
}

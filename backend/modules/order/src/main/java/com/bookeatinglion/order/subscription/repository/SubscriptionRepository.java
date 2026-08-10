package com.bookeatinglion.order.subscription.repository;

import com.bookeatinglion.order.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {}

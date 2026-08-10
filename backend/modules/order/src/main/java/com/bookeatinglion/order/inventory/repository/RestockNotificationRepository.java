package com.bookeatinglion.order.inventory.repository;

import com.bookeatinglion.order.inventory.domain.RestockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {}

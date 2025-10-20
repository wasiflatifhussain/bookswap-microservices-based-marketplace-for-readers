package com.bookswap.notification_service.repository;

import com.bookswap.notification_service.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {}

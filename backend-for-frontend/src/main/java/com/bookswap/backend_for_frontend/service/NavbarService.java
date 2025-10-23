package com.bookswap.backend_for_frontend.service;

import com.bookswap.backend_for_frontend.client.notification.NotificationClient;
import com.bookswap.backend_for_frontend.client.notification.dto.NotificationDto;
import com.bookswap.backend_for_frontend.client.wallet.WalletClient;
import com.bookswap.backend_for_frontend.client.wallet.dto.WalletDto;
import com.bookswap.backend_for_frontend.dto.response.NavbarSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class NavbarService {
  private final WalletClient walletClient;
  private final NotificationClient notificationClient;

  public NavbarSnapshot getSnapshot(Authentication authentication) {
    String userEmail = currentUserEmailOrNull();
    String userId = authentication.getName();
    List<String> errors = new ArrayList<>();

    WalletDto walletDto = null;
    Integer unreadNotifications = null;

    try {
      walletDto = walletClient.getMyBalance();
      log.info("Successfully fetched wallet balance for user={}", userId);
    } catch (Exception e) {
      log.error("Failed to fetch wallet balance for user={} with error={}", userId, e.getMessage());
      errors.add("wallet");
    }

    try {
      unreadNotifications = notificationClient.getUnreadCount();
      log.info("Successfully fetched unread notifications count for user={}", userId);
    } catch (Exception e) {
      log.error(
          "Failed to fetch unread notifications count for userId={} with error={}",
          userId,
          e.getMessage());
      errors.add("notifications");
    }

    String status = errors.isEmpty() ? "OK" : (errors.size() == 1 ? "PARTIAL" : "FAILED");

    return NavbarSnapshot.builder()
        .userId(userId)
        .userEmail(userEmail)
        .walletAvailableAmount(walletDto != null ? walletDto.getAvailableAmount() : 0.0f)
        .walletReservedAmount(walletDto != null ? walletDto.getReservedAmount() : 0.0f)
        .unreadNotificationCount(unreadNotifications != null ? unreadNotifications : 0)
        .status(status)
        .message(
            errors.isEmpty()
                ? "All data fetched successfully"
                : "Failed to fetch data: " + String.join(", ", errors))
        .build();
  }

  public List<NotificationDto> getNotifications(boolean unreadOnly, int page, int size) {
    try {
      List<NotificationDto> notificationDtoList =
          notificationClient.getNotifications(unreadOnly, page, size);
      log.info("Successfully fetched {} notifications for user", notificationDtoList.size());
      return notificationDtoList;
    } catch (Exception e) {
      log.error("Failed to fetch notifications for user with error={}", e.getMessage());
      return new ArrayList<>();
    }
  }

  public void markNotificationsAsRead(List<String> unreadNotifIds) {
    try {
      notificationClient.markNotificationsAsRead(unreadNotifIds);
      log.info("Successfully marked {} notifications as read for user", unreadNotifIds.size());
    } catch (Exception e) {
      log.error("Failed to mark notifications as read for user with error={}", e.getMessage());
    }
  }

  private String currentUserEmailOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return null;
    Object details = auth.getDetails();
    if (details instanceof Map<?, ?> map) {
      Object e = map.get("email");
      return e != null ? e.toString() : null;
    }
    return null;
  }
}

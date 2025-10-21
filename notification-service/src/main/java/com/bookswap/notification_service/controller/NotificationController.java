package com.bookswap.notification_service.controller;

import com.bookswap.notification_service.dto.response.NotificationItem;
import com.bookswap.notification_service.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping("/get")
  public List<NotificationItem> getNotifications(
      @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      Authentication authentication) {
    return notificationService.getNotifications(authentication.getName(), unreadOnly, page, size);
  }

  @PostMapping("/read")
  public void markNotificationsAsRead(
      @RequestBody List<String> notificationIds, Authentication authentication) {
    notificationService.markNotificationsAsRead(authentication.getName(), notificationIds);
  }

  @GetMapping("/unread-count")
  public Integer getUnreadNotificationCount(Authentication authentication) {
    return notificationService.getUnreadNotificationCount(authentication.getName());
  }
}

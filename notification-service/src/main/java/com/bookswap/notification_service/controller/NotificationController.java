package com.bookswap.notification_service.controller;

import com.bookswap.notification_service.dto.response.NotificationItem;
import com.bookswap.notification_service.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
      JwtAuthenticationToken authentication) {
    String userId = authentication.getToken().getSubject();
    return notificationService.getNotifications(userId, unreadOnly, page, size);
  }

  @PostMapping("/read")
  public void markNotificationsAsRead(
      @RequestBody List<String> notificationIds, JwtAuthenticationToken authentication) {
    String userId = authentication.getToken().getSubject();
    notificationService.markNotificationsAsRead(userId, notificationIds);
  }

  @GetMapping("/unread-count")
  public Integer getUnreadNotificationCount(JwtAuthenticationToken authentication) {
    String userId = authentication.getToken().getSubject();
    return notificationService.getUnreadNotificationCount(userId);
  }
}

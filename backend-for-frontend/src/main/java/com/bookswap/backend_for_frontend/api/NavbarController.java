package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.client.notification.dto.NotificationDto;
import com.bookswap.backend_for_frontend.dto.response.NavbarSnapshot;
import com.bookswap.backend_for_frontend.service.NavbarService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Controller for handling navbar snapshots: wallet, unread notifications, email */
@RestController
@RequestMapping("/api/bff/navbar")
@AllArgsConstructor
@Slf4j
public class NavbarController {
  private final NavbarService navbarService;

  @GetMapping("/snapshot")
  public ResponseEntity<NavbarSnapshot> snapshot(Authentication authentication) {
    NavbarSnapshot snapshot = navbarService.getSnapshot(authentication);
    return switch (snapshot.getStatus()) {
      case "OK" -> ResponseEntity.ok(snapshot);
      case "PARTIAL" -> ResponseEntity.status(206).body(snapshot);
      default -> ResponseEntity.status(500).body(snapshot);
    };
  }

  /** Fetch Notifications for current user; mark unread notifications as read */
  @GetMapping("/notifications")
  public ResponseEntity<List<NotificationDto>> getNotifications(
      @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(navbarService.getNotifications(unreadOnly, page, size));
  }

  @PostMapping("/notifications/read")
  public ResponseEntity<Void> markNotificationsAsRead(@RequestBody List<String> unreadNotifIds) {
    navbarService.markNotificationsAsRead(unreadNotifIds);
    return ResponseEntity.noContent().build();
  }
}

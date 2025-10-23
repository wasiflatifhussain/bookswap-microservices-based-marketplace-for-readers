package com.bookswap.backend_for_frontend.client.notification;

import com.bookswap.backend_for_frontend.client.notification.dto.NotificationDto;
import com.bookswap.backend_for_frontend.config.ServiceEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NotificationClient {
  private WebClient webClient;

  public NotificationClient(WebClient.Builder builder, ServiceEndpoints serviceEndpoints) {
    this.webClient = builder.baseUrl(serviceEndpoints.getNotification()).build();
  }

  public Integer getUnreadCount() {
    return webClient
        .get()
        .uri("/api/notifications/unread-count")
        .retrieve()
        .bodyToMono(Integer.class)
        .block();
  }

  public List<NotificationDto> getNotifications(boolean unreadOnly, int page, int size) {
    return webClient
        .get()
        .uri(
            "/api/notifications/get?unreadOnly={unreadOnly}&page={page}&size={size}",
            unreadOnly,
            page,
            size)
        .retrieve()
        .bodyToFlux(NotificationDto.class)
        .collectList()
        .block();
  }

  public void markNotificationsAsRead(List<String> unreadNotifIds) {
    webClient
        .post()
        .uri("/api/notifications/read")
        .bodyValue(unreadNotifIds)
        .retrieve()
        .bodyToMono(Void.class)
        .block();
  }
}

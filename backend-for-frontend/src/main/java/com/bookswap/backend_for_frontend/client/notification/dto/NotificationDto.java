package com.bookswap.backend_for_frontend.client.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
  private String notificationId;
  private String userId;
  private String notificationType;
  private String title;
  private String description;
  private String readStatus;
}

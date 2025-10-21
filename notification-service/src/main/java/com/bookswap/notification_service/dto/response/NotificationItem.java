package com.bookswap.notification_service.dto.response;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationItem {
  private String notificationId;
  private String userId;
  private String notificationType;
  private String title;
  String description;
  String readStatus;
}

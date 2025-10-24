package com.bookswap.backend_for_frontend.dto.navbar.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NavbarSnapshotDto {
  private String userId;
  private String userEmail;
  private Float walletAvailableAmount;
  private Float walletReservedAmount;
  private Integer unreadNotificationCount;
  private String message;
  String status; // "OK | "PARTIAL" | "ERROR"
}

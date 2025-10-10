package com.bookswap.wallet_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResponse {
  private String userId;
  private Float availableAmount;
  private Float reservedAmount;
  private String message;
}

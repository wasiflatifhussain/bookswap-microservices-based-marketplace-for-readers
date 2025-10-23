package com.bookswap.backend_for_frontend.client.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalance {
  private String userId;
  private Float availableAmount;
  private Float reservedAmount;
  private String message;
}

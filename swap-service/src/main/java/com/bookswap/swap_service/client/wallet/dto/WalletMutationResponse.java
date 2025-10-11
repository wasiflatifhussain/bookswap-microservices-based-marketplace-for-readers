package com.bookswap.swap_service.client.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletMutationResponse {
  private String userId;
  private String bookId;
  private String swapId;

  private Float amount;
  private String mutationType;

  private Float availableAmount;
  private Float reservedAmount;

  private String message;
}

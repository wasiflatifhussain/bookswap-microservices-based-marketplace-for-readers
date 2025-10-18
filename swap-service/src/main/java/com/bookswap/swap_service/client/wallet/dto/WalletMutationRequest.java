package com.bookswap.swap_service.client.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletMutationRequest {
  private String bookId;
  private String swapId;
  private Float amount;

  private String mutationType;
}

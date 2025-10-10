package com.bookswap.wallet_service.dto.response;

import com.bookswap.wallet_service.dto.MutationType;
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
  private MutationType mutationType;

  private Float availableAmount;
  private Float reservedAmount;

  private String message;
}

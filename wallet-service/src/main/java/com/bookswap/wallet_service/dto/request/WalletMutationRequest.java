package com.bookswap.wallet_service.dto.request;

import com.bookswap.wallet_service.dto.MutationType;
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

  private MutationType mutationType;
}

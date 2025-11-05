package com.bookswap.backend_for_frontend.client.swap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptSwapDto {
  private String swapId;
  private String responderUserId;
}

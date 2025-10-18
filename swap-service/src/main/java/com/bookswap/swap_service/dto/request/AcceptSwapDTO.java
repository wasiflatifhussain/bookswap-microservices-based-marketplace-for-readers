package com.bookswap.swap_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptSwapDTO {
  private String swapId;
  private String responderUserId;
}

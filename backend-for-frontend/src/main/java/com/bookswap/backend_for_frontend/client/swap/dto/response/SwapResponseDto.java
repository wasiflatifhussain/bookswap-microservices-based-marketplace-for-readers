package com.bookswap.backend_for_frontend.client.swap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapResponseDto {
  private String swapId;
  private String requesterUserId;
  private String responderUserId;
  private String requesterBookId;
  private String responderBookId;
  private String swapStatus;
  private SwapBookDto requesterBook;
  private SwapBookDto responderBook;
  private String message;
}

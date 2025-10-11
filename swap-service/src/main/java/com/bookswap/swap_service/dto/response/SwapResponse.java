package com.bookswap.swap_service.dto.response;

import com.bookswap.swap_service.client.catalog.dto.BookResponseWithMedia;
import com.bookswap.swap_service.domain.swap.SwapStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapResponse {
  private String swapId;
  private String requesterUserId;
  private String responderUserId;
  private String requesterBookId;
  private String responderBookId;
  private SwapStatus swapStatus;
  private BookResponseWithMedia requesterBook;
  private BookResponseWithMedia responderBook;
  private String message;
}

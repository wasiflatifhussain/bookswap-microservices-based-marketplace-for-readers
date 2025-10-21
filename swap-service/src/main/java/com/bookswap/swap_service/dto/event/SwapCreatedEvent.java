package com.bookswap.swap_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapCreatedEvent {
  private String swapId;
  private String requesterUserId;
  private String responderUserId;
  private String requesterBookId;
  private String responderBookId;
  private String responderBookName;
}

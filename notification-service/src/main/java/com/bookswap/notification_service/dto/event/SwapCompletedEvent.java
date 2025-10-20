package com.bookswap.notification_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapCompletedEvent {
  private String swapId;
  private String requesterUserId;
  private String responderUserId;
  private String requesterBookId;
  private String responderBookId;
}

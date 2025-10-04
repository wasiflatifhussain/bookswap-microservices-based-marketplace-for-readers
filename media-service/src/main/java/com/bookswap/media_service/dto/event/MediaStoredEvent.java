package com.bookswap.media_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaStoredEvent {
  private String bookId;
  private String ownerUserId;
  private String mediaId;
}

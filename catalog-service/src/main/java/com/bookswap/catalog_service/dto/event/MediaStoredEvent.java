package com.bookswap.catalog_service.dto.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStoredEvent {
  private String bookId;
  private String ownerUserId;
  private List<String> mediaIds;
  private int count;
}

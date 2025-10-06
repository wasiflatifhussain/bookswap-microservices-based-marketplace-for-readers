package com.bookswap.media_service.dto.upload;

import com.bookswap.media_service.domain.media.Status;
import java.util.List;
import lombok.Builder;

@Builder
public record CompleteResponse(String bookId, int totalCount, int successCount, List<Item> items) {

  @Builder
  public record Item(String mediaId, String bookId, Status status, String message) {}
}

package com.bookswap.catalog_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookSimpleResponse {
  private String bookId;
  private String title;
  private Float valuation;
  private String ownerUserId;

  // NOTE: message to provide context
  private String message;
}

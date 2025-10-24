package com.bookswap.backend_for_frontend.client.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookSimpleDto {
  private String bookId;
  private String title;
  private Float valuation;
  private String ownerUserId;
  private String message;
}

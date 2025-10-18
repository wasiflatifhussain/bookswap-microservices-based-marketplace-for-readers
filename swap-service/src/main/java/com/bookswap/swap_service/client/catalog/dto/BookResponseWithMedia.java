package com.bookswap.swap_service.client.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookResponseWithMedia {
  private String bookId;
  private String title;
  private String description;
  private String author;
  private Float valuation;
  private String ownerUserId;
  private String primaryMediaId;
}

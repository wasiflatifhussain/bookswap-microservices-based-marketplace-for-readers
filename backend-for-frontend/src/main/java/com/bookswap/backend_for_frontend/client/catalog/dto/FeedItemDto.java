package com.bookswap.backend_for_frontend.client.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedItemDto {
  private String bookId;
  private String title;
  private String description;
  private String genre;
  private String author;
  private String bookCondition;
  private Float valuation;
  private String bookStatus;
  private String thumbnailUrl;
}

package com.bookswap.backend_for_frontend.client.swap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapBookDto {
  private String bookId;
  private String title;
  private String description;
  private String author;
  private Float valuation;
  private String ownerUserId;
  private String primaryMediaId;
}

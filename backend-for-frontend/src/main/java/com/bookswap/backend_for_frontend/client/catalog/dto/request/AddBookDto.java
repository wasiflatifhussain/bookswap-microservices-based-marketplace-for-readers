package com.bookswap.backend_for_frontend.client.catalog.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddBookDto {
  private String title;
  private String description;
  private String genre;
  private String author;
  private String bookCondition;
  private Float valuation;
  private List<String> mediaIds;
}

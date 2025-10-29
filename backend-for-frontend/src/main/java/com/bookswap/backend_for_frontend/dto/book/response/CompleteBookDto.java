package com.bookswap.backend_for_frontend.dto.book.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteBookDto {
  private String bookId;
  private String title;
  private String description;
  private String genre;
  private String author;
  private String bookCondition;
  private Float valuation;
  private String bookStatus;
  private String ownerUserId;
  private List<String> mediaUrls;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

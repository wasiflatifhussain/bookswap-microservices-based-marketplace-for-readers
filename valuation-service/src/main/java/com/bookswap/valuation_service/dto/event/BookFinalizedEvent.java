package com.bookswap.valuation_service.dto.event;

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
public class BookFinalizedEvent {
  private String bookId;
  private String title;
  private String description;
  private String genre;
  private String author;
  private String bookCondition;
  private Float valuation;
  private String bookStatus;
  private List<String> mediaIds;
  private String ownerUserId; // NOTE: store Keycloak ID for users
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

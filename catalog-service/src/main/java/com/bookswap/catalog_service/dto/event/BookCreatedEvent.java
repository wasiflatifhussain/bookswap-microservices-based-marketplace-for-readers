package com.bookswap.catalog_service.dto.event;

import com.bookswap.catalog_service.domain.book.BookCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookCreatedEvent {
  private String bookId;
  private String title;
  private String description;
  private String author;
  private BookCondition bookCondition;
  private String ownerUserId;
}

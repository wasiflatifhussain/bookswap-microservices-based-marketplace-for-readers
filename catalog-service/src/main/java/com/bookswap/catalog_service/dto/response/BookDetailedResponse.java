package com.bookswap.catalog_service.dto.response;

import com.bookswap.catalog_service.domain.book.BookCondition;
import com.bookswap.catalog_service.domain.book.BookGenre;
import com.bookswap.catalog_service.domain.book.BookStatus;
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
public class BookDetailedResponse {
  private String bookId;

  private String title;
  private String description;

  private BookGenre genre;

  private String author;

  private BookCondition bookCondition;

  private Float valuation;

  private BookStatus bookStatus;

  private List<String> mediaIds; // TODO: convert to actual media ids
  private String ownerUserId; // NOTE: store Keycloak ID for users

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private String message;
}

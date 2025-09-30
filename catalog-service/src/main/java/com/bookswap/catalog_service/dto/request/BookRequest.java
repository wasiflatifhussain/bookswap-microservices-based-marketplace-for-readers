package com.bookswap.catalog_service.dto.request;

import com.bookswap.catalog_service.domain.book.BookCondition;
import com.bookswap.catalog_service.domain.book.BookGenre;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookRequest {
  @NotBlank(message = "Book title is required")
  private String title;

  @NotBlank(message = "Book description is required")
  private String description;

  @NotNull(message = "Book Genre is required")
  private BookGenre genre;

  @NotBlank(message = "Book author is required")
  private String author;

  @NotNull(message = "Book condition is required")
  private BookCondition bookCondition;

  @NotNull(message = "Book Value is required")
  @DecimalMin(value = "-0.1", inclusive = false, message = "Valuation must be greater than 0")
  private Float valuation;

  private List<String> mediaIds; // TODO: convert to actual media ids
}

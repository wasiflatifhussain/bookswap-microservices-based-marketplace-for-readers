package com.bookswap.backend_for_frontend.dto.home.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompletionConfirmDto {
  private String bookId;
  private int totalCount;
  private int successCount;
}

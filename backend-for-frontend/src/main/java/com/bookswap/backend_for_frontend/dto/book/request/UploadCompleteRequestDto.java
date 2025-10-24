package com.bookswap.backend_for_frontend.dto.book.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadCompleteRequestDto {
  private String bookId;
  private List<String> mediaIds;
}

package com.bookswap.backend_for_frontend.dto.book.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileItemDto {
  private String clientRef;
  private String name;
  private long sizeBytes;
  private String mimeType;
}

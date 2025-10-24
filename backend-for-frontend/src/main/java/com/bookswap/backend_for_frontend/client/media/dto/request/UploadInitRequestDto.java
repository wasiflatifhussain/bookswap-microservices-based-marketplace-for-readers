package com.bookswap.backend_for_frontend.client.media.dto.request;

import com.bookswap.backend_for_frontend.dto.book.request.FileItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadInitRequestDto {
  @JsonProperty("files")
  private List<FileItemDto> files;
}

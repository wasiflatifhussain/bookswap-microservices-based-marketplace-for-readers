package com.bookswap.backend_for_frontend.client.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaViewDto {
  private String mediaId;
  private String mimeType;
  private String presignedUrl;
}

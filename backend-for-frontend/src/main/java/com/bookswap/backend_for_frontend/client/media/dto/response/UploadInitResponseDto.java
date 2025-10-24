package com.bookswap.backend_for_frontend.client.media.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadInitResponseDto {
  private String bookId;
  private List<Item> results; // match Media’s JSON key exactly

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Item {
    private String clientRef;
    private String status; // READY | FAILED
    private String mediaId;
    private String objectKey;
    private String presignedPutUrl;
    private Headers requiredHeaders;
    private OffsetDateTime expiresAt;
    private String errorCode;
    private String errorMessage;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Headers {
    private String contentType;
  }
}

package com.bookswap.media_service.dto.upload;

import java.time.OffsetDateTime;
import java.util.List;

public record UploadInitResponse(String bookId, List<Item> results) {

  public record Item(
      String clientRef,
      String status, // READY | FAILED
      String mediaId,
      String objectKey,
      String presignedPutUrl,
      Headers requiredHeaders,
      OffsetDateTime expiresAt,
      String errorCode,
      String errorMessage) {

    public static Item ready(
        String clientRef,
        String mediaId,
        String objectKey,
        String url,
        Headers headers,
        OffsetDateTime exp) {
      return new Item(clientRef, "READY", mediaId, objectKey, url, headers, exp, null, null);
    }

    public static Item failed(String clientRef, String code, String message) {
      return new Item(clientRef, "FAILED", null, null, null, null, null, code, message);
    }

    public record Headers(String contentType) {}
  }
}

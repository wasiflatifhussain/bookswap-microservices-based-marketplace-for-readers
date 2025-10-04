package com.bookswap.media_service.dto.upload;

import java.util.List;

public record UploadInitRequest(List<FileItem> files) {
  public record FileItem(String clientRef, String name, long sizeBytes, String mimeType) {}
}

package com.bookswap.media_service.dto.download;

import lombok.Builder;

@Builder
public record MediaViewResponse(String mediaId, String mimeType, String presignedUrl) {}

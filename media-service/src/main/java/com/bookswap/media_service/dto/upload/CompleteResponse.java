package com.bookswap.media_service.dto.upload;

import com.bookswap.media_service.domain.media.Status;
import lombok.Builder;

@Builder
public record CompleteResponse(String mediaId, String bookId, Status status, String message) {}

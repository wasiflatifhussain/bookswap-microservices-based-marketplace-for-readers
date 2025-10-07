package com.bookswap.valuation_service.client.media.dto;

import lombok.Builder;

@Builder
public record MediaResponse(String mediaId, String mimeType, String presignedUrl) {}

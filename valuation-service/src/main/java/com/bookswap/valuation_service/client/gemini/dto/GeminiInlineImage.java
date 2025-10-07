package com.bookswap.valuation_service.client.gemini.dto;

import lombok.Builder;

@Builder
public record GeminiInlineImage(String mimeType, String base64Data) {}

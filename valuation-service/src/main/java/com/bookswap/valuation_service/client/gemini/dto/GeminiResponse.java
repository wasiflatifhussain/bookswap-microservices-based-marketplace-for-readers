package com.bookswap.valuation_service.client.gemini.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiResponse {
  private String status;
  private String rawJson;
  private Float bookCoins;
  private String comments;
  private String errorMessage;
}

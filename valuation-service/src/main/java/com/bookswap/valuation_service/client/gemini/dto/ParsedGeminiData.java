package com.bookswap.valuation_service.client.gemini.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedGeminiData {
  Float bookCoins;
  String comments;
}

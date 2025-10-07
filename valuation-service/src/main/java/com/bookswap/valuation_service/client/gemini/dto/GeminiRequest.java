package com.bookswap.valuation_service.client.gemini.dto;

import java.util.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiRequest {
  private List<Map<String, Object>> contents;
  private List<Map<String, Object>> tools;
  private Map<String, Object> generationConfig;

  public static GeminiRequest fromTextAndInlineImages(String text, List<GeminiInlineImage> images) {
    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("text", text));
    for (GeminiInlineImage img : images) {
      Map<String, Object> inlineData =
          Map.of(
              "mimeType", img.mimeType(),
              "data", img.base64Data());
      parts.add(Map.of("inlineData", inlineData));
    }
    Map<String, Object> content = Map.of("role", "user", "parts", parts);
    return GeminiRequest.builder()
        .contents(List.of(content))
        .generationConfig(defaultJsonGenConfig())
        .build();
  }

  public static GeminiRequest fromTextImagesAndSearch(String text, List<GeminiInlineImage> images) {
    GeminiRequest geminiRequest = fromTextAndInlineImages(text, images);
    // Enable search tool
    geminiRequest.setTools(List.of(Map.of("googleSearch", Map.of())));
    return geminiRequest;
  }

  private static Map<String, Object> defaultJsonGenConfig() {
    // Ensure API returns raw JSON output
    return Map.of("responseMimeType", "application/json");
  }
}

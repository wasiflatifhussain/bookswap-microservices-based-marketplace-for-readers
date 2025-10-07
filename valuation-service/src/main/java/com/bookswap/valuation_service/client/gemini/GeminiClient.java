package com.bookswap.valuation_service.client.gemini;

import com.bookswap.valuation_service.client.gemini.dto.GeminiInlineImage;
import com.bookswap.valuation_service.client.gemini.dto.GeminiRequest;
import com.bookswap.valuation_service.client.gemini.dto.GeminiResponse;
import com.bookswap.valuation_service.client.gemini.dto.ParsedGeminiData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GeminiClient {
  private final WebClient geminiWebClient;
  private final ObjectMapper mapper; // local mapper for parsing

  @Value("${gemini.api.url}")
  private String apiUrl;

  @Autowired
  public GeminiClient(
      @Qualifier("geminiWebClient") WebClient geminiWebClient, ObjectMapper mapper) {
    this.geminiWebClient = geminiWebClient;
    this.mapper = mapper;
  }

  public GeminiResponse getAnswerWithImagesAndSearch(
      String prompt, List<GeminiInlineImage> images) {
    GeminiRequest geminiRequest = GeminiRequest.fromTextImagesAndSearch(prompt, images);
    return executeClientCall(geminiRequest);
  }

  private GeminiResponse executeClientCall(GeminiRequest geminiRequest) {
    log.info("Sending request to Gemini API (with images/search enabled)");

    try {
      String rawOutput =
          geminiWebClient
              .post()
              .uri(apiUrl)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(geminiRequest)
              .retrieve()
              .onStatus(
                  statusCode -> statusCode.is4xxClientError() || statusCode.is5xxServerError(),
                  clientResponse ->
                      clientResponse
                          .bodyToMono(String.class)
                          .flatMap(
                              error -> {
                                log.error(
                                    "Gemini API Error with statusCode={} and Error={}",
                                    clientResponse.statusCode(),
                                    error);
                                return Mono.error(
                                    new RuntimeException("Gemini API error=" + error));
                              }))
              .bodyToMono(String.class)
              .block();

      // Extract the model’s text (JSON format) from the envelope
      String modelText = extractFirstText(rawOutput);
      // Defensive fallback: strip markdown fences if present
      modelText = stripMarkdownFences(modelText);
      modelText = ensureValidJson(modelText);
      ParsedGeminiData parsedGeminiData = parseBookCoinsAndComments(modelText);

      // Log full JSON
      log.info("Gemini API full response: {}", modelText);
      log.info(
          "Gemini parsed bookCoins={} comments={}",
          parsedGeminiData.getBookCoins(),
          parsedGeminiData.getComments());

      // Return structured response
      return mapToGeminiResponse(
          "SUCCESS",
          modelText,
          parsedGeminiData.getBookCoins(),
          parsedGeminiData.getComments(),
          "");
    } catch (Exception e) {
      log.error("Failed during Gemini API call: {}", e.getMessage(), e);
      return mapToGeminiResponse(
          "ERROR", null, null, null, "Exception during Gemini API call: " + e.getMessage());
    }
  }

  private String extractFirstText(String raw) {
    if (raw == null || raw.isBlank()) return "";
    try {
      JsonNode root = mapper.readTree(raw);
      JsonNode candidates = root.path("candidates");
      if (candidates.isArray() && candidates.size() > 0) {
        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (parts.isArray() && parts.size() > 0) {
          // Concatenate any text parts
          StringBuilder stringBuilder = new StringBuilder();
          for (JsonNode p : parts) {
            if (p.has("text")) stringBuilder.append(p.get("text").asText());
          }
          return stringBuilder.toString();
        }
      }
    } catch (Exception ignore) {
      // fall through to raw
    }
    // If schema unexpected, return raw
    return raw;
  }

  private String stripMarkdownFences(String string) {
    if (string == null) return "";

    // remove leading ```json or ``` and trailing ```
    String trimmed = string.trim();
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      if (firstNewline != -1) {
        trimmed = trimmed.substring(firstNewline + 1);
      }
      if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3);
      }
    }
    return trimmed.trim();
  }

  private String ensureValidJson(String text) {
    if (text == null || text.isBlank()) {
      return "{}";
    }

    String trimmed = text.trim();

    // Fix case where response starts without opening brace
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
      trimmed = "{" + trimmed;
    }

    // Check for multiple concatenated JSON objects and take only the first complete one
    try {
      int depth = 0;
      boolean inString = false;
      boolean escaped = false;
      int endIndex = -1;

      for (int i = 0; i < trimmed.length(); i++) {
        char c = trimmed.charAt(i);

        if (escaped) {
          escaped = false;
          continue;
        }

        if (inString) {
          if (c == '\\') {
            escaped = true;
          } else if (c == '"') {
            inString = false;
          }
          continue;
        }

        switch (c) {
          case '"':
            inString = true;
            break;
          case '{':
            depth++;
            break;
          case '}':
            depth--;
            if (depth == 0) {
              endIndex = i + 1;
              break;
            }
            break;
        }

        if (endIndex != -1) {
          break;
        }
      }

      if (endIndex > 0) {
        trimmed = trimmed.substring(0, endIndex);
      }

      // Validate the JSON by parsing it
      mapper.readTree(trimmed);
      return trimmed;
    } catch (Exception e) {
      log.warn("Failed to fix JSON structure: {}, returning original", e.getMessage());
      return text;
    }
  }

  private String cleanCommentText(String comment) {
    if (comment == null || comment.isEmpty()) {
      return "";
    }

    String cleaned = comment.replaceAll("[\n\r\t]", " ");
    cleaned = cleaned.replaceAll("\\s+", " ");
    cleaned = cleaned.replace("\"", "");
    return cleaned.trim();
  }

  private ParsedGeminiData parseBookCoinsAndComments(String modelText) {
    Float bookCoins = null;
    String comments = "";

    try {
      JsonNode root = mapper.readTree(modelText);
      if (root != null && root.isObject()) {
        // Support both key casings for resilience
        JsonNode bcNode = root.has("bookCoins") ? root.get("bookCoins") : root.get("bookcoins");
        if (bcNode != null) {
          if (bcNode.isNumber()) {
            bookCoins = bcNode.floatValue();
          } else if (bcNode.isTextual()) {
            try {
              bookCoins = Float.parseFloat(bcNode.asText());
            } catch (NumberFormatException e) {
              log.warn("Could not parse bookCoins value: {}", bcNode.asText());
            }
          }
        }

        JsonNode commNode = root.get("comments");
        if (commNode != null && commNode.isTextual()) {
          comments = cleanCommentText(commNode.asText());
        }
      }
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse bookCoins/comments from Gemini JSON with error={}", e.getMessage());
    }

    return ParsedGeminiData.builder().bookCoins(bookCoins).comments(comments).build();
  }

  private GeminiResponse mapToGeminiResponse(
      String status, String rawJson, Float bookCoins, String comments, String errorMessage) {
    return GeminiResponse.builder()
        .status(status)
        .rawJson(rawJson)
        .bookCoins(bookCoins)
        .comments(comments)
        .errorMessage(errorMessage)
        .build();
  }
}

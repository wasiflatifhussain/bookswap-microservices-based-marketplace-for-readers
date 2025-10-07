package com.bookswap.valuation_service.service;

import com.bookswap.valuation_service.client.gemini.GeminiClient;
import com.bookswap.valuation_service.client.gemini.dto.GeminiInlineImage;
import com.bookswap.valuation_service.client.gemini.dto.GeminiResponse;
import com.bookswap.valuation_service.client.media.MediaDownloadClient;
import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import com.bookswap.valuation_service.dto.event.BookFinalizedEvent;
import com.bookswap.valuation_service.service.prompt.ValuationPromptProvider;
import com.fasterxml.jackson.core.JsonParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiCallerService {

  private final GeminiClient geminiClient;
  private final MediaDownloadClient mediaDownloadClient;
  private final ValuationPromptProvider valuationPromptProvider;

  @Retryable(
      retryFor = {WebClientResponseException.class, RuntimeException.class},
      noRetryFor = {IllegalArgumentException.class, JsonParseException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 15000))
  public GeminiResponse generateValuation(
      BookFinalizedEvent bookFinalizedEvent, List<MediaResponse> mediaResponseList) {
    log.info("Generating valuation for bookId={}", bookFinalizedEvent.getBookId());

    try {
      // Build prompt with book details and media URLs
      String geminiPrompt =
          valuationPromptProvider.buildPrompt(bookFinalizedEvent, mediaResponseList);

      // Convert presigned URLs -> Inline base64 image data
      List<GeminiInlineImage> images = mediaDownloadClient.downloadImages(mediaResponseList);

      GeminiResponse geminiResponse =
          geminiClient.getAnswerWithImagesAndSearch(geminiPrompt, images);

      log.info(
          "Gemini rawJson length={} for bookCoins={} comments={}",
          geminiResponse.getRawJson() != null ? geminiResponse.getRawJson().length() : 0,
          geminiResponse.getBookCoins(),
          geminiResponse.getComments());

      return geminiResponse;
    } catch (Exception e) {
      log.error(
          "Error generating Gemini Response for bookId={} : {}",
          bookFinalizedEvent.getBookId(),
          e.getMessage(),
          e);
      throw e;
    }
  }

  @Recover
  public GeminiResponse recoverFromFailure(
      Exception ex, BookFinalizedEvent book, List<MediaResponse> media) {
    log.error(
        "All retry attempts failed for bookId={} : {}",
        book != null ? book.getBookId() : "unknown",
        ex.getMessage(),
        ex);
    return GeminiResponse.builder()
        .status("ERROR")
        .rawJson(
            "{\"status\":\"ERROR\",\"message\":\"Failed to get valuation from AI after retries\"}")
        .bookCoins(null)
        .comments(null)
        .errorMessage("Failed after retries: " + ex.getMessage())
        .build();
  }
}

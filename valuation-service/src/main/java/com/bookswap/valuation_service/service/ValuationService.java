package com.bookswap.valuation_service.service;

import com.bookswap.valuation_service.client.gemini.dto.GeminiResponse;
import com.bookswap.valuation_service.client.media.MediaServiceClient;
import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import com.bookswap.valuation_service.domain.outbox.AggregateType;
import com.bookswap.valuation_service.domain.valuation.Valuation;
import com.bookswap.valuation_service.dto.event.BookFinalizedEvent;
import com.bookswap.valuation_service.dto.event.BookUnlistedEvent;
import com.bookswap.valuation_service.repository.ValuationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationService {
  private final ValuationRepository valuationRepository;
  private final OutboxService outboxService;

  private final MediaServiceClient mediaServiceClient;
  private final GeminiCallerService geminiCallerService;

  @Transactional
  public void computeBookValuation(BookFinalizedEvent bookFinalizedEvent) {
    try {
      Optional<Valuation> existingValuation =
          valuationRepository.findByBookId(bookFinalizedEvent.getBookId());

      if (existingValuation.isPresent()) {
        log.info("Valuation already exists for bookId={}", bookFinalizedEvent.getBookId());
        return;
      }

      log.info("Book received for valuation with bookFinalizedEvent={}", bookFinalizedEvent);
      List<MediaResponse> mediaResponseList =
          mediaServiceClient.getMediaByBookId(bookFinalizedEvent.getBookId()).block();

      log.info("Successfully fetch media for book as mediaResponseList={}", mediaResponseList);

      GeminiResponse geminiResponse =
          geminiCallerService.generateValuation(bookFinalizedEvent, mediaResponseList);
      log.info(
          "Gemini response for bookId={} is geminiResponse={}",
          bookFinalizedEvent.getBookId(),
          geminiResponse);

      Valuation valuation = mapToValuationObj(bookFinalizedEvent, geminiResponse);
      valuationRepository.save(valuation);

      outboxService.enqueueEvent(
          AggregateType.VALUATION, valuation.getBookId(), "VALUATION_COMPUTED", valuation);

    } catch (Exception e) {
      log.error("Failed to compute valuation for bookId={}", bookFinalizedEvent.getBookId(), e);
    }
  }

  private Valuation mapToValuationObj(
      BookFinalizedEvent bookFinalizedEvent, GeminiResponse geminiResponse) {
    return Valuation.builder()
        .bookId(bookFinalizedEvent.getBookId())
        .ownerUserId(bookFinalizedEvent.getOwnerUserId())
        .bookCoinValue(geminiResponse.getBookCoins())
        .comments(geminiResponse.getComments())
        .build();
  }

  @Transactional
  public void deleteValuationByBookId(String bookId, String ownerUserId) {
    log.info("Deleting valuation for bookId={} for ownerUserId={}", bookId, ownerUserId);

    try {
      Optional<Valuation> existingValuation = valuationRepository.findByBookId(bookId);
      if (existingValuation.isEmpty()) {
        log.info("No books exist for bookId={}", bookId);
        return;
      }

      BookUnlistedEvent unlistedEvent =
          BookUnlistedEvent.builder()
              .bookId(bookId)
              .ownerUserId(ownerUserId)
              .valuation(existingValuation.get().getBookCoinValue())
              .build();

      valuationRepository.deleteByBookId(bookId);
      outboxService.enqueueEvent(
          AggregateType.VALUATION, bookId, "VALUATION_DELETED", unlistedEvent);

      log.info(
          "Deleted from DB and published VALUATION_DELETED for bookId={} for ownerUserId={}",
          bookId,
          ownerUserId);
    } catch (Exception e) {
      log.error("Error deleting valuation for bookId={}:", bookId, e);
    }
  }
}

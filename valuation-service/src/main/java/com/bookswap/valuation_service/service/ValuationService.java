package com.bookswap.valuation_service.service;

import com.bookswap.valuation_service.domain.outbox.AggregateType;
import com.bookswap.valuation_service.domain.valuation.Valuation;
import com.bookswap.valuation_service.dto.event.BookFinalizedEvent;
import com.bookswap.valuation_service.dto.event.BookUnlistedEvent;
import com.bookswap.valuation_service.repository.ValuationRepository;
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

  @Transactional
  public void computeBookValuation(BookFinalizedEvent bookFinalizedEvent) {
    try {
      Optional<Valuation> existingValuation =
          valuationRepository.findByBookId(bookFinalizedEvent.getBookId());
      if (existingValuation.isPresent()) {
        log.info("Valuation already exists for bookId={}", bookFinalizedEvent.getBookId());
        return;
      }

      log.info("Book received for valuation={}", bookFinalizedEvent);

      // TODO: perform API call and perform book valuation
      Float bookCoinValue = 10.00F;
      String comments = "Test";
      Valuation valuation = mapToValuationObj(bookFinalizedEvent, bookCoinValue, comments);
      valuationRepository.save(valuation);

      outboxService.enqueueEvent(
          AggregateType.VALUATION, valuation.getBookId(), "VALUATION_COMPUTED", valuation);

    } catch (Exception e) {
      log.error("Failed to compute valuation for bookId={}", bookFinalizedEvent.getBookId(), e);
    }
  }

  private Valuation mapToValuationObj(
      BookFinalizedEvent bookFinalizedEvent, Float bookCoinValue, String comments) {
    return Valuation.builder()
        .bookId(bookFinalizedEvent.getBookId())
        .ownerUserId(bookFinalizedEvent.getOwnerUserId())
        .bookCoinValue(bookCoinValue)
        .comments(comments)
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
          BookUnlistedEvent.builder().bookId(bookId).ownerUserId(ownerUserId).build();

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

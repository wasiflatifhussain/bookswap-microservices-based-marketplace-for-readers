package com.bookswap.notification_service.service;

import com.bookswap.notification_service.domain.BookSnapshot;
import com.bookswap.notification_service.dto.event.BookFinalizedEvent;
import com.bookswap.notification_service.dto.event.BookUnlistedEvent;
import com.bookswap.notification_service.repository.BookSnapshotRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class BookSnapshotService {
  private final BookSnapshotRepository bookSnapshotRepository;

  @Transactional
  public void storeBookSnapshot(BookFinalizedEvent bookFinalizedEvent) {
    log.info("Storing book snapshot for bookId={}", bookFinalizedEvent.getBookId());

    try {
      BookSnapshot bookSnapshot =
          BookSnapshot.builder()
              .bookId(bookFinalizedEvent.getBookId())
              .title(bookFinalizedEvent.getTitle())
              .author(bookFinalizedEvent.getAuthor())
              .valuation(bookFinalizedEvent.getValuation())
              .bookCondition(bookFinalizedEvent.getBookCondition())
              .ownerUserId(bookFinalizedEvent.getOwnerUserId())
              .build();
      bookSnapshotRepository.save(bookSnapshot);
      log.info("Successfully stored book snapshot for bookId={}", bookFinalizedEvent.getBookId());
    } catch (Exception ex) {
      log.error("Failed to store book snapshot for bookId={}", bookFinalizedEvent.getBookId(), ex);
      throw new RuntimeException(ex);
    }
  }

  @Transactional
  public void deleteBookSnapshot(BookUnlistedEvent bookUnlistedEvent) {
    log.info("Deleting book snapshot for bookId={}", bookUnlistedEvent.getBookId());

    try {
      Optional<BookSnapshot> bookSnapshotOpt =
          bookSnapshotRepository.findByBookId(bookUnlistedEvent.getBookId());
      if (bookSnapshotOpt.isEmpty()) {
        log.warn("No book snapshot found to delete for bookId={}", bookUnlistedEvent.getBookId());
        return;
      }
      bookSnapshotRepository.deleteByBookId(bookUnlistedEvent.getBookId());
      log.info("Successfully deleted book snapshot for bookId={}", bookUnlistedEvent.getBookId());
    } catch (Exception ex) {
      log.error("Failed to delete book snapshot for bookId={}", bookUnlistedEvent.getBookId(), ex);
      throw new RuntimeException(ex);
    }
  }
}

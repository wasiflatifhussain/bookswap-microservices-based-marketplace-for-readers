package com.bookswap.notification_service.messaging;

import com.bookswap.notification_service.dto.event.BookFinalizedEvent;
import com.bookswap.notification_service.dto.event.BookUnlistedEvent;
import com.bookswap.notification_service.service.BookSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCatalogEventsListener {
  private final ObjectMapper objectMapper;
  private final BookSnapshotService bookSnapshotService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.catalog-service.topic}",
      groupId = "${spring.kafka.consumer.services.catalog-service.group-id}")
  public void onCatalogEvent(ConsumerRecord<String, String> rec) {
    final String eventType = header(rec, "eventType");
    final String source = header(rec, "source");

    try {
      if (eventType == null) {
        log.warn(
            "Skipping catalog event without eventType header: key={} value={}",
            rec.key(),
            rec.value());
        return;
      }

      switch (eventType) {
        case "BOOK_VALUATION_FINALIZED" -> {
          BookFinalizedEvent bookFinalizedEvent =
              objectMapper.readValue(rec.value(), BookFinalizedEvent.class);
          log.info(
              "BOOK_VALUATION_FINALIZED event received for bookId={}",
              bookFinalizedEvent.getBookId());
          bookSnapshotService.storeBookSnapshot(bookFinalizedEvent);
        }
        case "BOOK_UNLISTED" -> {
          BookUnlistedEvent bookUnlistedEvent =
              objectMapper.readValue(rec.value(), BookUnlistedEvent.class);
          log.info("BOOK_UNLISTED event received for bookId={}", bookUnlistedEvent.getBookId());
          bookSnapshotService.deleteBookSnapshot(bookUnlistedEvent);
        }
        default -> {
          log.debug("Ignored catalog eventType={} key={}", eventType, rec.key());
        }
      }
    } catch (Exception ex) {
      log.error(
          "Failed to process catalog event: eventType={} key={} value={}",
          eventType,
          rec.key(),
          rec.value(),
          ex);
    }
  }

  private static String header(ConsumerRecord<String, String> rec, String key) {
    var h = rec.headers().lastHeader(key);
    return (h == null) ? null : new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
  }
}

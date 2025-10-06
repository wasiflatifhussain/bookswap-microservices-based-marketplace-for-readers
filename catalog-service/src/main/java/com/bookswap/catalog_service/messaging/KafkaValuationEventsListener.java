package com.bookswap.catalog_service.messaging;

import com.bookswap.catalog_service.dto.event.ValuationComputedEvent;
import com.bookswap.catalog_service.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaValuationEventsListener {
  private final ObjectMapper objectMapper;
  private final BookService bookService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.valuation-service.topic}", // e.g. media.events
      groupId = "${spring.kafka.consumer.services.valuation-service.group-id}")
  public void onValuationEvent(ConsumerRecord<String, String> rec) {
    final String eventType = header(rec, "eventType");
    final String source = header(rec, "source"); // optional; see anti-loop note below

    try {
      if (eventType == null) {
        log.warn(
            "Skipping valuation event without eventType header: key={} value={}",
            rec.key(),
            rec.value());
        return;
      }

      switch (eventType) {
        case "VALUATION_COMPUTED" -> {
          ValuationComputedEvent e =
              objectMapper.readValue(rec.value(), ValuationComputedEvent.class);
          log.info(
              "VALUATION_COMPUTED bookId={} for ownerId={} with bookCoinValue=",
              e.getBookId(),
              e.getOwnerUserId(),
              e.getBookCoinValue());
          // idempotent update (BookService should upsert only if missing)
          bookService.appendValuationToBook(
              e.getBookId(), e.getOwnerUserId(), e.getBookCoinValue());
        }
        default -> {
          log.debug("Ignored media eventType={} key={}", eventType, rec.key());
        }
      }

    } catch (Exception ex) {
      log.error("Failed to process media event key={} value={}", rec.key(), rec.value(), ex);
    }
  }

  private static String header(ConsumerRecord<String, String> rec, String key) {
    var h = rec.headers().lastHeader(key);
    return (h == null) ? null : new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
  }
}

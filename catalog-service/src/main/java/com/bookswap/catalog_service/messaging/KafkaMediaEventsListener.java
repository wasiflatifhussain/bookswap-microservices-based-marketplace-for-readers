package com.bookswap.catalog_service.messaging;

import com.bookswap.catalog_service.dto.event.MediaStoredEvent;
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
public class KafkaMediaEventsListener {

  private final ObjectMapper objectMapper;
  private final BookService bookService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.media-service.topic}", // e.g. media.events
      groupId = "${spring.kafka.consumer.services.media-service.group-id}")
  public void onMediaEvent(ConsumerRecord<String, String> rec) {
    final String eventType = header(rec, "eventType");
    final String source = header(rec, "source"); // optional; see anti-loop note below

    try {
      if (eventType == null) {
        log.warn(
            "Skipping media event without eventType header: key={} value={}",
            rec.key(),
            rec.value());
        return;
      }

      switch (eventType) {
        case "MEDIA_STORED" -> {
          MediaStoredEvent e = objectMapper.readValue(rec.value(), MediaStoredEvent.class);
          log.info("MEDIA_STORED bookId={} mediaId={}", e.getBookId(), e.getMediaIds());
          // idempotent update (BookService should upsert only if missing)
          bookService.appendMediaToBook(e.getBookId(), e.getOwnerUserId(), e.getMediaIds());
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

package com.bookswap.email_service.messaging;

import com.bookswap.email_service.dto.event.BookCreatedEvent;
import com.bookswap.email_service.service.EmailService;
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
  private final EmailService emailService;

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
        case "BOOK_CREATED" -> {
          BookCreatedEvent e = objectMapper.readValue(rec.value(), BookCreatedEvent.class);
          log.info(
              "BOOK_CREATED for userId={} emailAddress={}", e.getOwnerUserId(), e.getOwnerEmail());
          emailService.upsertUserInfo(e.getOwnerUserId(), e.getOwnerEmail());
        }
        default -> {
          log.debug("Ignored book eventType={} key={}", eventType, rec.key());
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

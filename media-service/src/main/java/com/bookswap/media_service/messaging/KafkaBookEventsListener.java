package com.bookswap.media_service.messaging;

import com.bookswap.media_service.dto.event.BookUnlistedEvent;
import com.bookswap.media_service.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookEventsListener {

  private final ObjectMapper objectMapper;
  private final MediaService mediaService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.catalog-service.topic}",
      groupId = "${spring.kafka.consumer.services.catalog-service.group-id}")
  public void handleBookEvents(ConsumerRecord<String, String> consumerRecord) {
    try {
      String eventType = header(consumerRecord, "eventType");
      String aggregateType = header(consumerRecord, "aggregateType");

      if (eventType == null) {
        log.warn(
            "Skipping event without eventType header: key={} value={}",
            consumerRecord.key(),
            consumerRecord.value());
        return;
      }

      switch (eventType) {
        case "BOOK_UNLISTED" -> {
          // value holds the payload JSON
          BookUnlistedEvent event =
              objectMapper.readValue(consumerRecord.value(), BookUnlistedEvent.class);

          log.info(
              "BOOK_UNLISTED received (bookId={}, ownerId={}, key={}, aggregateType={})",
              event.getBookId(),
              event.getOwnerUserId(),
              consumerRecord.key(),
              aggregateType);

          mediaService.deleteMediaByBookId(event.getBookId(), event.getOwnerUserId());
        }

        default -> {
          // NOTE: since this is debug, it won't print on terminal as log level is set to info
          log.debug("Ignored catalog eventType={} key={}", eventType, consumerRecord.key());
        }
      }

    } catch (Exception e) {
      log.error(
          "Failed processing catalog event key={} value={}",
          consumerRecord.key(),
          consumerRecord.value(),
          e);
    }
  }

  private static String header(ConsumerRecord<String, String> rec, String key) {
    Header h = rec.headers().lastHeader(key);
    return (h == null) ? null : new String(h.value(), StandardCharsets.UTF_8);
  }
}

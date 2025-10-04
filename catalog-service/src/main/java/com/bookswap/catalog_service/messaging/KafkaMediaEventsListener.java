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
      topics = "${spring.kafka.consumer.media-service.topic}",
      groupId = "${spring.kafka.consumer.media-service.group-id}")
  public void handleMediaStoredEvent(ConsumerRecord<String, String> consumerRecord) {
    try {
      String value = consumerRecord.value();
      MediaStoredEvent event =
          objectMapper.readValue(
              value,
              MediaStoredEvent.class); // deserializes the JSON string into a MediaStoredEvent
      log.info(
          "Received MEDIA_STORED for bookId={} and mediaId={}",
          event.getBookId(),
          event.getMediaId());

      bookService.appendMediaToBook(event.getBookId(), event.getOwnerUserId(), event.getMediaId());
    } catch (Exception e) {
      log.error("Failed to process media event={} with e=", consumerRecord, e);
    }
  }
}

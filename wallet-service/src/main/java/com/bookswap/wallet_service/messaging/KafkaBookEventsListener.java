package com.bookswap.wallet_service.messaging;

import com.bookswap.wallet_service.dto.event.BookFinalizedEvent;
import com.bookswap.wallet_service.dto.event.BookUnlistedEvent;
import com.bookswap.wallet_service.service.WalletService;
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
  private final WalletService valuationService;

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
        case "BOOK_VALUATION_FINALIZED" -> {
          BookFinalizedEvent event =
              objectMapper.readValue(consumerRecord.value(), BookFinalizedEvent.class);

          log.info(
              "BOOK_VALUATION_FINALIZED received (bookId={}, ownerUserId={}, valuation={}, key={}, aggregateType={})",
              event.getBookId(),
              event.getOwnerUserId(),
              event.getValuation(),
              consumerRecord.key(),
              aggregateType);
          valuationService.addToUserWallet(event.getOwnerUserId(), event);
        }
        case "BOOK_UNLISTED" -> {
          BookUnlistedEvent event =
              objectMapper.readValue(consumerRecord.value(), BookUnlistedEvent.class);

          log.info(
              "BOOK_UNLISTED received (bookId={}, ownerUserId={}, valuation={}, key={}, aggregateType={})",
              event.getBookId(),
              event.getOwnerUserId(),
              event.getValuation(),
              consumerRecord.key(),
              aggregateType);
          valuationService.deleteFromUserWallet(event.getOwnerUserId(), event);
        }
        default -> {
          // Record for testing
          log.info("Received book event: {}", consumerRecord.value());
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

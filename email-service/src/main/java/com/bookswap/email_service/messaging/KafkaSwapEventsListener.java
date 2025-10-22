package com.bookswap.email_service.messaging;

import com.bookswap.email_service.dto.event.SwapCompletedEvent;
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
public class KafkaSwapEventsListener {

  private final ObjectMapper objectMapper;
  private final EmailService emailService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.swap-service.topic}",
      groupId = "${spring.kafka.consumer.services.swap-service.group-id}")
  public void onSwapEvent(ConsumerRecord<String, String> rec) {
    final String eventType = header(rec, "eventType");
    final String source = header(rec, "source");

    try {
      if (eventType == null) {
        log.warn(
            "Skipping swap event without eventType header: key={} value={}",
            rec.key(),
            rec.value());
        return;
      }

      switch (eventType) {
        case "SWAP_COMPLETED" -> {
          SwapCompletedEvent e = objectMapper.readValue(rec.value(), SwapCompletedEvent.class);
          log.info(
              "SWAP_COMPLETED event for swapId={} requesterUserId={} responderUserId={}",
              e.getSwapId(),
              e.getRequesterUserId(),
              e.getResponderUserId());
          emailService.sendEmail(e.getSwapId(), e.getRequesterUserId(), e.getResponderUserId());
        }
        default -> {
          log.debug("Ignored swap eventType={} key={}", eventType, rec.key());
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

package com.bookswap.notification_service.messaging;

import com.bookswap.notification_service.dto.event.SwapCancelEvent;
import com.bookswap.notification_service.dto.event.SwapCompletedEvent;
import com.bookswap.notification_service.dto.event.SwapCreatedEvent;
import com.bookswap.notification_service.service.NotificationService;
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
  private final NotificationService notificationService;

  @KafkaListener(
      topics = "${spring.kafka.consumer.services.swap-service.topic}",
      groupId = "${spring.kafka.consumer.services.swap-service.group-id}")
  public void onSwapEvent(ConsumerRecord<String, String> rec) {
    final String eventType = header(rec, "eventType");
    final String source = header(rec, "source");

    try {
      if (eventType == null) {
        log.warn(
            "Skipping media event without eventType header: key={} value={}",
            rec.key(),
            rec.value());
        return;
      }

      switch (eventType) {
        case "SWAP_CREATED" -> {
          SwapCreatedEvent swapCreatedEvent =
              objectMapper.readValue(rec.value(), SwapCreatedEvent.class);
          log.info("SWAP_CREATED event received for swapId={}", swapCreatedEvent.getSwapId());
          notificationService.addNotificationToResponder(swapCreatedEvent);
        }
        case "SWAP_CANCELLED" -> {
          SwapCancelEvent swapCancelEvent =
              objectMapper.readValue(rec.value(), SwapCancelEvent.class);

          log.info("SWAP_CANCELLED event received for swapId={}", swapCancelEvent.getSwapId());
          notificationService.addNotificationToRequester(swapCancelEvent);
        }
        case "SWAP_COMPLETED" -> {
          SwapCompletedEvent swapCompletedEvent =
              objectMapper.readValue(rec.value(), SwapCompletedEvent.class);

          log.info("SWAP_COMPLETED event received for swapId={}", swapCompletedEvent.getSwapId());
          notificationService.addNotificationToBothUsers(swapCompletedEvent);
        }
        default -> {
          log.debug("Ignored swap eventType={} key={}", eventType, rec.key());
        }
      }
    } catch (Exception ex) {
      log.error(
          "Failed to process swap event with eventType={} key={} value={}",
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

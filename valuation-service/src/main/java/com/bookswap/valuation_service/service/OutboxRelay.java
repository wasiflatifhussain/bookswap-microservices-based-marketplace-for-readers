package com.bookswap.valuation_service.service;

import com.bookswap.valuation_service.domain.outbox.OutboxEvent;
import com.bookswap.valuation_service.domain.outbox.OutboxStatus;
import com.bookswap.valuation_service.messaging.KafkaOutboxPublisher;
import com.bookswap.valuation_service.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {
  private final OutboxEventRepository outboxEventRepository;
  private final KafkaOutboxPublisher kafkaOutboxPublisher;
  private static final int BATCH_SIZE = 50;

  @Scheduled(
      fixedDelayString =
          "${outbox.relay.fixedDelayMs:10000}") // send every second: RECONSIDER time set
  @Transactional
  public void publishPendingEvents() {
    List<OutboxEvent> eventBatch = outboxEventRepository.claimPendingEvents(BATCH_SIZE);
    log.info("Claimed no. of outbox events for processing={}", eventBatch.size());

    for (OutboxEvent e : eventBatch) {
      try {
        outboxEventRepository.markAttempt(
            e.getOutboxEventId(), OutboxStatus.IN_PROGRESS, LocalDateTime.now());

        kafkaOutboxPublisher
            .publish(
                e.getAggregateType(),
                e.getEventType(),
                e.getAggregateId(),
                e.getOutboxEventId(),
                e.getOutboxPayloadJson())
            .join();

        log.info("Successfully published outbox event with id={}", e.getOutboxEventId());

        // NOTE: since we modify a managed JPA entity inside a @Transactional method,
        // changes will be auto-detected and persisted at transaction commit time.
        // So, no explicit save() call is needed here.
        e.setStatus(OutboxStatus.SENT);
        e.setSentAt(LocalDateTime.now());

      } catch (Exception ex) {
        log.error(
            "Error while processing outbox event with ={} and error=", e.getOutboxEventId(), ex);
        // marks attempt and saves failed time & sets status to PENDING for retry in next batch
        outboxEventRepository.markAttempt(
            e.getOutboxEventId(), OutboxStatus.PENDING, LocalDateTime.now());
      }
    }
  }
}

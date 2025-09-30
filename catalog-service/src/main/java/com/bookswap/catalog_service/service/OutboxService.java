package com.bookswap.catalog_service.service;

import com.bookswap.catalog_service.domain.outbox.AggregateType;
import com.bookswap.catalog_service.domain.outbox.OutboxEvent;
import com.bookswap.catalog_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public void enqueueEvent(
      AggregateType aggregateType, String aggregateId, String eventType, Object eventPayload) {
    try {
      String payload = objectMapper.writeValueAsString(eventPayload);
      OutboxEvent outboxEvent =
          OutboxEvent.builder()
              .aggregateType(aggregateType)
              .aggregateId(aggregateId)
              .eventType(eventType)
              .outboxPayloadJson(payload)
              .build();
      outboxEventRepository.save(outboxEvent);
    } catch (Exception e) {
      throw new RuntimeException("Failed to enqueue outbox event", e);
    }
  }
}

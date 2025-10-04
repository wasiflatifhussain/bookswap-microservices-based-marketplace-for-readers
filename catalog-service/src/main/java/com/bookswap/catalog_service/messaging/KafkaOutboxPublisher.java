package com.bookswap.catalog_service.messaging;

import com.bookswap.catalog_service.domain.outbox.AggregateType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOutboxPublisher {

  @Value("${spring.kafka.producer.services.catalog-service.topic}")
  private String kafkaTopic;

  private final KafkaTemplate<String, String> kafkaTemplate;

  // error handling and retries required
  public CompletableFuture<Void> publish(
      AggregateType aggregateType,
      String eventType,
      String aggregateId,
      String outboxEventId,
      String jsonPayload) {

    try {
      ProducerRecord<String, String> record =
          new ProducerRecord<>(kafkaTopic, aggregateId, jsonPayload);
      record
          .headers()
          .add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)))
          .add(
              new RecordHeader(
                  "aggregateType", aggregateType.name().getBytes(StandardCharsets.UTF_8)))
          .add(new RecordHeader("outboxEventId", outboxEventId.getBytes(StandardCharsets.UTF_8)));

      CompletableFuture<SendResult<String, String>> completableFuture = kafkaTemplate.send(record);

      return completableFuture.thenAccept(
          result -> {
            var metadata = result.getRecordMetadata();
            log.info(
                "Kafka Message Published with topic={} partition={} offset={} key={}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset(),
                aggregateId);
          });
    } catch (Exception ex) {
      log.error("Error while publishing to Kafka for outboxEventId={}", outboxEventId, ex);
      CompletableFuture<Void> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(ex);
      return failedFuture;
    }
  }
}

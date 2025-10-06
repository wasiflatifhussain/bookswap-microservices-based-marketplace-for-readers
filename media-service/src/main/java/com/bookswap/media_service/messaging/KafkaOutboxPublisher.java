package com.bookswap.media_service.messaging;

import com.bookswap.media_service.domain.outbox.AggregateType;
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

  @Value("${spring.kafka.producer.services.media-service.topic}")
  private String kafkaTopic;

  private final KafkaTemplate<String, String> kafkaTemplate;

  /**
   * Publish an outbox event to Kafka.
   *
   * @param aggregateType e.g. MEDIA
   * @param eventType e.g. MEDIA_STORED
   * @param aggregateId the aggregate id the event is about (mediaId)
   * @param outboxEventId db id for the outbox row (for tracing/idempotency)
   * @param jsonPayload serialized JSON payload
   * @return future that completes with the send result (or exceptionally on failure)
   */
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
          .add(new RecordHeader("eventType", bytes(eventType)))
          .add(new RecordHeader("aggregateType", bytes(aggregateType.name())))
          .add(new RecordHeader("aggregateId", bytes(aggregateId)))
          .add(new RecordHeader("outboxEventId", bytes(outboxEventId)));

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

  private static byte[] bytes(String s) {
    return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
  }
}

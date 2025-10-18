package com.bookswap.swap_service.domain.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String outboxEventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AggregateType aggregateType;

  @Column(nullable = false)
  private String aggregateId; // responderUserId for partitioning

  @Column(nullable = false)
  private String eventType;

  @Lob
  @Column(nullable = false)
  private String outboxPayloadJson;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  @Builder.Default
  private OutboxStatus status = OutboxStatus.PENDING;

  @CreationTimestamp private LocalDateTime createdAt;
  @UpdateTimestamp private LocalDateTime updatedAt;

  private LocalDateTime lastAttemptAt;
  @Builder.Default private Integer attemptCount = 0;

  private LocalDateTime sentAt;
}

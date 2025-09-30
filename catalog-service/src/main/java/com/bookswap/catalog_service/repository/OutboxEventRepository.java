package com.bookswap.catalog_service.repository;

import com.bookswap.catalog_service.domain.outbox.OutboxEvent;
import com.bookswap.catalog_service.domain.outbox.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

  @Query(
      value =
          """
      SELECT * FROM outbox_events
      WHERE status = 'PENDING'
      ORDER BY created_at
      LIMIT :batchSize
      FOR UPDATE SKIP LOCKED
      """,
      nativeQuery = true)
  List<OutboxEvent> claimPendingEvents(@Param("batchSize") int batchSize);

  /** Marks and maintains attempts for changing status and refetching from DB for failed events* */
  @Modifying
  @Query(
      "update OutboxEvent e set e.status = :status, e.lastAttemptAt = :now, e.attemptCount = e.attemptCount + 1 where e.outboxEventId = :id")
  void markAttempt(
      @Param("id") String outboxEventId,
      @Param("status") OutboxStatus status,
      @Param("now") LocalDateTime now);
}

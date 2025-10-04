package com.bookswap.media_service.domain.outbox;

public enum OutboxStatus {
  PENDING,
  IN_PROGRESS,
  SENT,
  FAILED
}

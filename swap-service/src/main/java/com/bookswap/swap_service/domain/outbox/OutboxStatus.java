package com.bookswap.swap_service.domain.outbox;

public enum OutboxStatus {
  PENDING,
  IN_PROGRESS,
  SENT,
  FAILED
}

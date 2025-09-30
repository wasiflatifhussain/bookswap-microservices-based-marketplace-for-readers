package com.bookswap.catalog_service.domain.outbox;

public enum OutboxStatus {
  PENDING,
  IN_PROGRESS,
  SENT,
  FAILED
}

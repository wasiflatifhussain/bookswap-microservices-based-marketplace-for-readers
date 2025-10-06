package com.bookswap.catalog_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValuationComputedEvent {
  private String valuationId;
  private String bookId;
  private String ownerUserId;
  private Float bookCoinValue;
  private String comments;
}

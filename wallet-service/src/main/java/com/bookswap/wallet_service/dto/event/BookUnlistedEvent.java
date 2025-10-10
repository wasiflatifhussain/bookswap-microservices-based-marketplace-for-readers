package com.bookswap.wallet_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookUnlistedEvent {
  private String bookId;
  private String ownerUserId;
  private Float valuation;
}

package com.bookswap.email_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookCreatedEvent {
  private String bookId;
  private String ownerUserId;
  private String ownerEmail;
}

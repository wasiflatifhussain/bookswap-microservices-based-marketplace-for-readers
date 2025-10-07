package com.bookswap.valuation_service.domain.valuation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valuation")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Valuation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String valuationId;

  @Column(nullable = false)
  private String bookId;

  @Column(nullable = false)
  private String ownerUserId;

  @Column(nullable = false)
  private Float bookCoinValue;

  @Lob private String comments;
}

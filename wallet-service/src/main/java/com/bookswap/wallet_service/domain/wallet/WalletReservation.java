package com.bookswap.wallet_service.domain.wallet;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallet_reservations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String reservationId;

  @Column(nullable = false)
  private String userId;

  @Column(nullable = false)
  private String bookId;

  @Column(nullable = false)
  private String swapId;

  @Column(nullable = false)
  private Float amount;

  @CreationTimestamp private LocalDateTime createdAt;

  @UpdateTimestamp private LocalDateTime updatedAt;
}

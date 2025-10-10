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
@Table(name = "wallets")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String walletId;

  @Column(nullable = false)
  private String userId;

  @Column(nullable = false)
  private Float availableAmount;

  @Column(nullable = false)
  private Float reservedAmount;

  @CreationTimestamp private LocalDateTime createdAt;
  @UpdateTimestamp private LocalDateTime updatedAt;
}

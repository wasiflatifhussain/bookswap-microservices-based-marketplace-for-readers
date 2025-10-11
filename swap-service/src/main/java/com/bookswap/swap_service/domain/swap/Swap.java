package com.bookswap.swap_service.domain.swap;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "swaps")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Swap {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String swapId;

  @Column(nullable = false)
  private String requesterUserId;

  @Column(nullable = false)
  private String responderUserId;

  @Column(nullable = false)
  private String requesterBookId;

  @Column(nullable = false)
  private String responderBookId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SwapStatus swapStatus;

  @Column(nullable = false)
  private Float requesterBookPrice;

  @Column(nullable = false)
  private Float responderBookPrice;

  @CreationTimestamp private LocalDateTime createdAt;

  @UpdateTimestamp private LocalDateTime updatedAt;
}

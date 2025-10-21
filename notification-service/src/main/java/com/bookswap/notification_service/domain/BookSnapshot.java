package com.bookswap.notification_service.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "book_snapshots")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookSnapshot {
  @Id private String bookId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Column(nullable = false)
  private String bookCondition;

  @Column(nullable = false)
  private Float valuation;

  @Column(nullable = false)
  private String ownerUserId;

  @CreationTimestamp private LocalDateTime createdAt;

  @UpdateTimestamp private LocalDateTime updatedAt;
}

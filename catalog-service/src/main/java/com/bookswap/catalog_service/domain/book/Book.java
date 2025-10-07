package com.bookswap.catalog_service.domain.book;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "books")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Book {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String bookId;

  private String title;

  @Lob private String description;

  @Enumerated(EnumType.STRING)
  private BookGenre genre;

  private String author;

  @Enumerated(EnumType.STRING)
  private BookCondition bookCondition;

  private Float valuation;

  @Enumerated(EnumType.STRING)
  private BookStatus bookStatus;

  private List<String> mediaIds;
  private String ownerUserId;

  @CreationTimestamp private LocalDateTime createdAt;
  @UpdateTimestamp private LocalDateTime updatedAt;
}

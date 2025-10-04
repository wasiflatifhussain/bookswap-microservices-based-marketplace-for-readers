package com.bookswap.media_service.domain.media;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {
  @Id private String mediaId;

  private String bookId;
  private String ownerUserId;
  private String objectKey;

  private String mimeType;
  private Long sizeBytes;

  @Enumerated(EnumType.STRING)
  private Status status;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  /**
   * Runs before the entity is persisted (inserted) into the database. Sets the createdAt and
   * updatedAt
   */
  @PrePersist
  void prePersist() {
    var now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Runs before the entity is updated in the database. Updates the updatedAt timestamp. */
  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}

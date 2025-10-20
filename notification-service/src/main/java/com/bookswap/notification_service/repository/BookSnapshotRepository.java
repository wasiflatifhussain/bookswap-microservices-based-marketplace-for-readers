package com.bookswap.notification_service.repository;

import com.bookswap.notification_service.domain.BookSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookSnapshotRepository extends JpaRepository<BookSnapshot, String> {
  Optional<BookSnapshot> findByBookId(String requesterBookId);

  void deleteByBookId(String bookId);
}

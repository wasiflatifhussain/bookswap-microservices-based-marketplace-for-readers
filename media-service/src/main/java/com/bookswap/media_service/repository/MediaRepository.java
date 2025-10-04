package com.bookswap.media_service.repository;

import com.bookswap.media_service.domain.media.Media;
import com.bookswap.media_service.domain.media.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, String> {
  List<Media> findByBookIdAndStatus(String bookId, Status status);
}

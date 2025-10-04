package com.bookswap.media_service.repository;

import com.bookswap.media_service.domain.media.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, String> {}

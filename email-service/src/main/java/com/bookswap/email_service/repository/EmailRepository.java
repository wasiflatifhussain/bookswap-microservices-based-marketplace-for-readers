package com.bookswap.email_service.repository;

import com.bookswap.email_service.domain.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailRepository extends JpaRepository<Email, String> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO emails (user_id, email_address, created_at, updated_at)
          VALUES (:ownerUserId, :ownerEmail, NOW(), NOW())
          ON CONFLICT (user_id)
          DO UPDATE SET email_address = EXCLUDED.email_address, updated_at = NOW()
          """,
      nativeQuery = true)
  void upsertEmail(
      @Param("ownerUserId") String ownerUserId, @Param("ownerEmail") String ownerEmail);
}

package com.bookswap.email_service.service;

import com.bookswap.email_service.repository.EmailRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {
  private final EmailRepository emailRepository;

  @Transactional
  public void upsertUserInfo(String ownerUserId, String ownerEmail) {
    log.info("Initiating upsert for userId: {}, email: {}", ownerUserId, ownerEmail);

    try {
      emailRepository.upsertEmail(ownerUserId, ownerEmail);
      log.info("Successfully completed upsert for userId: {}, email: {}", ownerUserId, ownerEmail);
    } catch (Exception e) {
      log.error("Error during upsert for userId={}, email={}", ownerUserId, ownerEmail, e);
      throw new RuntimeException(e);
    }
  }
}

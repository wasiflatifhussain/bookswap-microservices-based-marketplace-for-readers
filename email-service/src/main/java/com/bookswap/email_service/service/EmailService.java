package com.bookswap.email_service.service;

import com.bookswap.email_service.client.ResendClient;
import com.bookswap.email_service.domain.Email;
import com.bookswap.email_service.repository.EmailRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {
  private final EmailRepository emailRepository;
  private final ResendClient resendClient;

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

  @Retryable(
      retryFor = {RuntimeException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
  @Transactional(readOnly = true)
  public void sendEmail(String swapId, String requesterUserId, String responderUserId) {
    log.info(
        "Initializing email sending for requesterUserId: {}, responderUserId: {}",
        requesterUserId,
        responderUserId);

    Optional<Email> requesterEmailOpt = emailRepository.findById(requesterUserId);
    Optional<Email> responderEmailOpt = emailRepository.findById(responderUserId);
    if (requesterEmailOpt.isEmpty() || responderEmailOpt.isEmpty()) {
      log.warn(
          "Email addresses not found for requesterUserId={} or responderUserId={}",
          requesterUserId,
          responderUserId);
      return;
    }
    String requesterEmail = requesterEmailOpt.get().getEmailAddress();
    String responderEmail = responderEmailOpt.get().getEmailAddress();

    // TODO: Integrate Resend and send email to both users
    String subject = "BookSwap – Swap " + swapId + " is SUCCESSFUL!";
    String htmlContent =
        """
            <html>
              <body style="font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:#333;margin:0;padding:16px;">
                <p>Dear BookSwap Users,</p>
                <p>We are pleased to inform you that your swap transaction <b>%s</b> has been successfully completed.</p>
                <p>To facilitate your exchange, we have created this email thread to allow both participants to communicate directly and arrange a convenient time and place for the face-to-face swap. Please use <b>Reply-All</b> for all correspondence related to this trade to ensure that communications remain secure and properly documented.</p>
                <ul style="margin-top:10px;margin-bottom:10px;">
                  <li><b>Participants:</b> %s, %s</li>
                </ul>
                <p><b>Safety Reminder:</b> We recommend meeting in a public location, verifying all items in person, and keeping communications within this thread for record and security purposes.</p>
                <p>Thank you for using <b>BookSwap</b>. We hope you have a smooth and enjoyable exchange experience.</p>
                <p>Best regards,<br/>The BookSwap Team</p>
                <p style="color:#888;font-size:12px;margin-top:20px;">Transactional email • Swap %s</p>
              </body>
            </html>
          """
            .formatted(swapId, requesterEmail, responderEmail, swapId);

    String textContent =
        """
            Dear BookSwap Users,

            Your swap transaction %s has been successfully completed.
            Please use Reply-All on this thread for all correspondence related to this trade.

            Participants: %s, %s

            Safety Reminder: meet in a public place, verify items in person, and keep communications within this thread for your records.

            — The BookSwap Team
            (Transactional email • Swap %s)
          """
            .formatted(swapId, requesterEmail, responderEmail, swapId);
    boolean emailSuccess =
        resendClient.sendEmailToBothUsers(
            swapId, subject, htmlContent, textContent, requesterEmail, responderEmail);
    if (!emailSuccess) {
      log.error("Resend returned false for swap: {}, triggering retry", swapId);
      throw new RuntimeException("Email send failed for swap: " + swapId);
    }

    log.info("Email successfully sent for swap: {}", swapId);
  }

  @Recover
  public void recoverFromEmailFailure(RuntimeException ex, String swapId, String req, String resp) {
    log.error("All retry attempts failed for swap: {} - {}", swapId, ex.getMessage());
  }
}

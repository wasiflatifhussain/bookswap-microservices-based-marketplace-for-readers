package com.bookswap.email_service.client;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResendClient {
  private final Resend resend;

  @Value("${email.resend.from-address}")
  private String fromAddress;

  public boolean sendEmailToBothUsers(
      String swapId,
      String subject,
      String htmlContent,
      String text,
      String user1Email,
      String user2Email) {
    log.info("Using Resend to send email to user1={} and user2={}", user1Email, user2Email);

    SendEmailRequest sendEmailRequest =
        SendEmailRequest.builder()
            .from(fromAddress)
            .to(List.of(user1Email, user2Email))
            .subject(subject)
            .html(htmlContent)
            .text(text)
            .headers(Map.of("List-Id", "swaps.bookswap.app", "X-BookSwap-Swap-Id", swapId))
            .build();

    try {
      SendEmailResponse sendEmailResponse = resend.emails().send(sendEmailRequest);
      log.info("Email sent successfully via Resend: id={}", sendEmailResponse.getId());
      return sendEmailResponse != null
          && sendEmailResponse.getId() != null
          && !sendEmailResponse.getId().isBlank();
    } catch (Exception e) {
      log.error(
          "Resend send failed to=[{}, {}] with error={}", user1Email, user2Email, e.getMessage());
      return false;
    }
  }
}

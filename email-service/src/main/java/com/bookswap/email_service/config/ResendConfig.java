package com.bookswap.email_service.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

  @Bean
  public Resend resend(@Value("${email.resend.api-key}") String apiKey) {
    return new Resend(apiKey); // one shared instance
  }
}

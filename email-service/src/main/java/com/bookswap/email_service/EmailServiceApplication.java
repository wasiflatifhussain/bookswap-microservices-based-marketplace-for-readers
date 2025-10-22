package com.bookswap.email_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class EmailServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailServiceApplication.class, args);
  }
}

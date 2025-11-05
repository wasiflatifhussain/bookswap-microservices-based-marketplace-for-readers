package com.bookswap.backend_for_frontend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.client-endpoints")
@Data
public class ServiceEndpoints {
  private String catalog, media, wallet, swap, notification;
}

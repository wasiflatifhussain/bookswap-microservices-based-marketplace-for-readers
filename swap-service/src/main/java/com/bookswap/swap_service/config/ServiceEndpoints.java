package com.bookswap.swap_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.client-endpoints")
@Data
public class ServiceEndpoints {
  private String catalog;
  private String wallet;
}

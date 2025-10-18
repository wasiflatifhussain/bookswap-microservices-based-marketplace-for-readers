package com.bookswap.swap_service.config;

import com.bookswap.swap_service.security.ServiceTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean("catalogServiceWebClient")
  public WebClient catalogServiceWebClient(
      WebClient.Builder builder, ServiceTokenProvider tokenProvider) {

    ExchangeFilterFunction authFilter =
        (request, next) -> {
          String token = tokenProvider.getToken();
          ClientRequest authenticated =
              ClientRequest.from(request).headers(h -> h.setBearerAuth(token)).build();
          return next.exchange(authenticated);
        };

    return builder.baseUrl("http://localhost:8081/api/catalog").filter(authFilter).build();
  }

  @Bean("walletServiceWebClient")
  public WebClient walletServiceWebClient(
      WebClient.Builder builder, ServiceTokenProvider tokenProvider) {

    ExchangeFilterFunction authFilter =
        (request, next) -> {
          String token = tokenProvider.getToken();
          ClientRequest authenticated =
              ClientRequest.from(request).headers(h -> h.setBearerAuth(token)).build();
          return next.exchange(authenticated);
        };

    return builder.baseUrl("http://localhost:8084/api/wallet").filter(authFilter).build();
  }
}

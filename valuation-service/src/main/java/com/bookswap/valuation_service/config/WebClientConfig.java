package com.bookswap.valuation_service.config;

import com.bookswap.valuation_service.security.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  /** Calls Media microservice (with bearer token, has baseUrl). */
  @Bean("mediaServiceWebClient")
  public WebClient mediaServiceWebClient(
      WebClient.Builder builder, ServiceTokenProvider tokenProvider) {

    ExchangeFilterFunction authFilter =
        (request, next) -> {
          String token = tokenProvider.getToken();
          ClientRequest authenticated =
              ClientRequest.from(request).headers(h -> h.setBearerAuth(token)).build();
          return next.exchange(authenticated);
        };

    return builder.baseUrl("http://localhost:8082/api/media").filter(authFilter).build();
  }

  /** Downloads presigned S3 URLs. */
  @Bean("mediaDownloadWebClient")
  public WebClient mediaDownloadWebClient(WebClient.Builder builder) {
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();

    return builder.exchangeStrategies(strategies).build();
  }

  /** Calls Gemini API */
  @Bean("geminiWebClient")
  public WebClient geminiWebClient(
      WebClient.Builder builder, @Value("${gemini.api.key}") String apiKey) {

    return builder
        .defaultHeader("x-goog-api-key", apiKey)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}

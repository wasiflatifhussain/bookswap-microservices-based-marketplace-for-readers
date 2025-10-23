package com.bookswap.backend_for_frontend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder(
      @Value("${http.timeoutMs:2000}") long timeoutMs,
      @Value("${http.maxInMemoryMb:16}") int maxInMemoryMb) {

    // timeout + compression
    HttpClient http =
        HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs)).compress(true);

    ExchangeFilterFunction tokenRelay =
        (req, next) -> {
          String authHeader;
          RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
          if (attrs instanceof ServletRequestAttributes sra) {
            authHeader = sra.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
          } else {
            authHeader = null;
          }
          ClientRequest outgoing =
              ClientRequest.from(req)
                  .headers(
                      h -> {
                        if (authHeader != null) h.set(HttpHeaders.AUTHORIZATION, authHeader);
                      })
                  .build();
          return next.exchange(outgoing);
        };

    // increase in-memory buffer for JSON payloads if needed
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(maxInMemoryMb * 1024 * 1024))
            .build();

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(http))
        .exchangeStrategies(strategies)
        .filter(tokenRelay);
  }
}

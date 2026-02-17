package com.bookswap.backend_for_frontend.config;

import com.bookswap.backend_for_frontend.security.session.SessionData;
import com.bookswap.backend_for_frontend.security.session.SessionRepository;
import jakarta.servlet.http.Cookie;
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

  private final SessionRepository sessionRepository;
  private final String cookieName;

  public WebClientConfig(
      SessionRepository sessionRepository,
      @Value("${auth.session.cookie-name}") String cookieName) {
    this.sessionRepository = sessionRepository;
    this.cookieName = cookieName;
  }

  @Bean
  public WebClient.Builder webClientBuilder(
      @Value("${http.timeoutMs:2000}") long timeoutMs,
      @Value("${http.maxInMemoryMb:16}") int maxInMemoryMb) {

    HttpClient httpClient =
        HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs)).compress(true);

    ExchangeFilterFunction tokenRelay =
        (request, next) -> {
          String authHeader = null;

          RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
          if (attrs instanceof ServletRequestAttributes) {
            // Ensure backward compatibility with non-reactive contexts
            ServletRequestAttributes sra = (ServletRequestAttributes) attrs;

            authHeader = sra.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

            // If no Authorization header, try to find token in session cookie
            if (authHeader == null) {
              Cookie[] cookies = sra.getRequest().getCookies();
              if (cookies != null) {
                for (Cookie cookie : cookies) {
                  if (cookieName.equals(cookie.getName())) {
                    SessionData session = sessionRepository.find(cookie.getValue());
                    if (session != null
                        && session.getFirebaseIdToken() != null
                        && session.getExpiresAt() != null
                        && session.getExpiresAt().isAfter(java.time.Instant.now())) {
                      authHeader = "Bearer " + session.getFirebaseIdToken();
                    }
                    break;
                  }
                }
              }
            }
          }

          String finalAuthHeader = authHeader;
          ClientRequest outgoing =
              ClientRequest.from(request)
                  .headers(
                      headers -> {
                        if (finalAuthHeader != null) {
                          headers.set(HttpHeaders.AUTHORIZATION, finalAuthHeader);
                        }
                      })
                  .build();

          return next.exchange(outgoing);
        };

    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(maxInMemoryMb * 1024 * 1024))
            .build();

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .filter(tokenRelay);
  }
}

package com.bookswap.valuation_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

  private final WebClient.Builder webClientBuilder;

  @Value("${keycloak.token-uri}")
  private String tokenUri;

  @Value("${keycloak.service-service-comms-client.client-id}")
  private String clientId;

  @Value("${keycloak.service-service-comms-client.client-secret}")
  private String clientSecret;

  private volatile String cachedToken;
  private volatile long expiresAtEpochMs = 0;

  public synchronized String getToken() {
    long now = System.currentTimeMillis();

    // reuse same token until 5 seconds before it actually expires
    // then, auto fetch a new token
    if (cachedToken != null && now < expiresAtEpochMs - 5000) {
      return cachedToken;
    }

    WebClient webClient = webClientBuilder.baseUrl(tokenUri).build();
    String requestBody =
        String.format(
            "grant_type=client_credentials&client_id=%s&client_secret=%s", clientId, clientSecret);

    TokenResponse tokenResponse =
        webClient
            .post()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .block();

    if (tokenResponse == null) {
      throw new IllegalStateException("Failed to fetch Keycloak service token");
    }

    cachedToken = tokenResponse.access_token();
    expiresAtEpochMs = now + (tokenResponse.expires_in() * 1000L);

    return cachedToken;
  }

  private record TokenResponse(String access_token, long expires_in, String token_type) {}
}

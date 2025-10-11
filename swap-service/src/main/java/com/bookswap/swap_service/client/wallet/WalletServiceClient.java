package com.bookswap.swap_service.client.wallet;

import com.bookswap.swap_service.client.wallet.dto.WalletMutationRequest;
import com.bookswap.swap_service.client.wallet.dto.WalletMutationResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class WalletServiceClient {
  private final WebClient walletServiceWebClient;

  public WalletServiceClient(
      @Qualifier("walletServiceWebClient") WebClient walletServiceWebClient) {
    this.walletServiceWebClient = walletServiceWebClient;
  }

  public Mono<WalletMutationResponse> reserveInRequestWallet(
      String userId, WalletMutationRequest walletMutationRequest) {
    log.info("Starting reserve call to Wallet-Service for userId={}", userId);

    return walletServiceWebClient
        .post()
        .uri("/{userId}/reserve", userId)
        .bodyValue(walletMutationRequest)
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(new ParameterizedTypeReference<WalletMutationResponse>() {});
              } else if (resp.statusCode().is4xxClientError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.warn(
                              "Wallet-Service 4xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalArgumentException("Wallet 4xx: " + body));
                        });
              } else if (resp.statusCode().is5xxServerError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.error(
                              "Wallet-Service 5xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalStateException("Wallet-Service error"));
                        });
              }
              return Mono.error(
                  new IllegalStateException("Unexpected HTTP status: " + resp.statusCode()));
            })
        .timeout(Duration.ofSeconds(10))
        .doOnError(e -> log.error("Failed reserving funds with error={}", e.toString()));
  }

  public Mono<WalletMutationResponse> releaseReservedInRequestWallet(
      String userId, WalletMutationRequest build) {
    log.info("Starting release call to Wallet-Service for userId={}", userId);

    return walletServiceWebClient
        .post()
        .uri("/{userId}/release", userId)
        .bodyValue(build)
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(new ParameterizedTypeReference<WalletMutationResponse>() {});
              } else if (resp.statusCode().is4xxClientError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.warn(
                              "Wallet-Service 4xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalArgumentException("Wallet 4xx: " + body));
                        });
              } else if (resp.statusCode().is5xxServerError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.error(
                              "Wallet-Service 5xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalStateException("Wallet-Service error"));
                        });
              }
              return Mono.error(
                  new IllegalStateException("Unexpected HTTP status: " + resp.statusCode()));
            })
        .timeout(Duration.ofSeconds(10))
        .doOnError(e -> log.error("Failed releasing funds with error={}", e.toString()));
  }
}

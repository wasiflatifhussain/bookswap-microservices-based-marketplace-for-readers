package com.bookswap.backend_for_frontend.client.wallet;

import com.bookswap.backend_for_frontend.client.wallet.dto.response.WalletDto;
import com.bookswap.backend_for_frontend.config.ServiceEndpoints;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WalletClient {
  private final WebClient webClient;

  public WalletClient(WebClient.Builder builder, ServiceEndpoints serviceEndpoints) {
    this.webClient = builder.baseUrl(serviceEndpoints.getWallet()).build();
  }

  // Blocking call
  public WalletDto getMyBalance() {
    return webClient
        .get()
        .uri("/api/wallet/me/balance")
        .retrieve()
        .bodyToMono(WalletDto.class)
        .block();
  }
}

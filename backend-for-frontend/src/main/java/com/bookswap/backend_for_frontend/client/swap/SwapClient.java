package com.bookswap.backend_for_frontend.client.swap;

import com.bookswap.backend_for_frontend.client.swap.dto.request.AcceptSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.request.CancelSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.request.CreateSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.response.SwapResponseDto;
import com.bookswap.backend_for_frontend.config.ServiceEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SwapClient {
  private WebClient webClient;

  public SwapClient(WebClient.Builder builder, ServiceEndpoints serviceEndpoints) {
    this.webClient = builder.baseUrl(serviceEndpoints.getSwap()).build();
  }

  public List<SwapResponseDto> getMySentSwapRequests(String userId) {
    // send to /api/swap/requests/sent with RequestParams requesterUserId=userId and
    // swapStatus=PENDING
    return webClient
        .get()
        .uri("/api/swap/requests/sent?requesterUserId={userId}&swapStatus=PENDING", userId)
        .retrieve()
        .bodyToFlux(SwapResponseDto.class)
        .collectList()
        .block();
  }

  public List<SwapResponseDto> getMyReceivedSwapRequests(String userId) {
    // send to /api/swap/requests/received with RequestParams responderUserId=userId and
    // swapStatus=PENDING
    return webClient
        .get()
        .uri("/api/swap/requests/received?responderUserId={userId}&swapStatus=PENDING", userId)
        .retrieve()
        .bodyToFlux(SwapResponseDto.class)
        .collectList()
        .block();
  }

  public List<SwapResponseDto> getRequestsForBook(String userId, String bookId) {
    // send to /api/swap/requests/for-book with RequestParams userId=userId and bookId=bookId
    return webClient
        .get()
        .uri("/api/swap/requests/for-book?userId={userId}&bookId={bookId}", userId, bookId)
        .retrieve()
        .bodyToFlux(SwapResponseDto.class)
        .collectList()
        .block();
  }

  public SwapResponseDto cancelSwapRequest(CancelSwapDto cancelSwapDto) {
    return webClient
        .post()
        .uri("/api/swap/requests/cancel")
        .bodyValue(cancelSwapDto)
        .retrieve()
        .bodyToMono(SwapResponseDto.class)
        .block();
  }

  public SwapResponseDto createSwapRequest(CreateSwapDto createSwapDto) {
    return webClient
        .post()
        .uri("/api/swap/requests/create")
        .bodyValue(createSwapDto)
        .retrieve()
        .bodyToMono(SwapResponseDto.class)
        .block();
  }

  public SwapResponseDto acceptSwapRequest(AcceptSwapDto acceptSwapDto) {
    return webClient
        .post()
        .uri("/api/swap/requests/accept")
        .bodyValue(acceptSwapDto)
        .retrieve()
        .bodyToMono(SwapResponseDto.class)
        .block();
  }
}

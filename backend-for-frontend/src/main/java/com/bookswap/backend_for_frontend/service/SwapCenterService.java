package com.bookswap.backend_for_frontend.service;

import com.bookswap.backend_for_frontend.client.swap.SwapClient;
import com.bookswap.backend_for_frontend.client.swap.dto.request.AcceptSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.request.CancelSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.request.CreateSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.response.SwapResponseDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SwapCenterService {
  private final SwapClient swapClient;

  public List<SwapResponseDto> getMySentSwapRequests(String userId) {
    log.info("Fetching my sent swap requests from Swap service");
    try {
      return swapClient.getMySentSwapRequests(userId);
    } catch (Exception e) {
      log.error("Failed to fetch my sent swap requests: {}", e.getMessage());
      return List.of();
    }
  }

  public List<SwapResponseDto> getMyReceivedSwapRequests(String userId) {
    log.info("Fetching my received swap requests from Swap service");
    try {
      return swapClient.getMyReceivedSwapRequests(userId);
    } catch (Exception e) {
      log.error("Failed to fetch my received swap requests: {}", e.getMessage());
      return List.of();
    }
  }

  public List<SwapResponseDto> getSwapRequestsForBook(String userId, String bookId) {
    log.info("Fetching swap requests for bookId={} from Swap service", bookId);
    try {
      return swapClient.getRequestsForBook(userId, bookId);
    } catch (Exception e) {
      log.error("Failed to fetch swap requests for bookId={} : {}", bookId, e.getMessage());
      return List.of();
    }
  }

  public SwapResponseDto cancelSwapRequest(String swapId, String userId) {
    log.info("Cancelling swap request with id={} for user={}", swapId, userId);
    try {
      CancelSwapDto cancelSwapDto =
          CancelSwapDto.builder().swapId(swapId).requesterUserId(userId).build();
      return swapClient.cancelSwapRequest(cancelSwapDto);
    } catch (Exception e) {
      log.error(
          "Failed to cancel swap request with id={} for user={} : {}",
          swapId,
          userId,
          e.getMessage());
      return SwapResponseDto.builder().build();
    }
  }

  public SwapResponseDto createSwapRequest(CreateSwapDto createSwapDto) {
    log.info(
        "Creating swap request for requesterBookId={} and responderBookId={}",
        createSwapDto.getRequesterBookId(),
        createSwapDto.getResponderBookId());
    try {
      return swapClient.createSwapRequest(createSwapDto);
    } catch (Exception e) {
      log.error(
          "Failed to create swap request for requesterBookId={} and responderBookId={} : {}",
          createSwapDto.getRequesterBookId(),
          createSwapDto.getResponderBookId(),
          e.getMessage());
      return SwapResponseDto.builder().build();
    }
  }

  public SwapResponseDto acceptSwapRequest(String swapId, String userId) {
    log.info("Accepting swap request with id={} for user={}", swapId, userId);
    try {
      AcceptSwapDto acceptSwapDto =
          AcceptSwapDto.builder().swapId(swapId).responderUserId(userId).build();
      return swapClient.acceptSwapRequest(acceptSwapDto);
    } catch (Exception e) {
      log.error(
          "Failed to accept swap request with id={} for user={} : {}",
          swapId,
          userId,
          e.getMessage());
      return SwapResponseDto.builder().build();
    }
  }
}

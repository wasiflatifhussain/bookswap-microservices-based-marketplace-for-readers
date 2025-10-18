package com.bookswap.swap_service.controller;

import com.bookswap.swap_service.domain.swap.SwapStatus;
import com.bookswap.swap_service.dto.request.AcceptSwapDTO;
import com.bookswap.swap_service.dto.request.CancelSwapDTO;
import com.bookswap.swap_service.dto.request.CreateSwapDTO;
import com.bookswap.swap_service.dto.response.SwapResponse;
import com.bookswap.swap_service.service.SwapService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/swap")
@AllArgsConstructor
@Slf4j
public class SwapController {
  private final SwapService swapService;

  @GetMapping("/requests/sent")
  public ResponseEntity<List<SwapResponse>> getUserRequests(
      @RequestParam String requesterUserId, @RequestParam SwapStatus swapStatus) {
    return ResponseEntity.ok(swapService.getUserRequests(requesterUserId, swapStatus));
  }

  @GetMapping("/requests/received")
  public ResponseEntity<List<SwapResponse>> getReceivedRequests(
      @RequestParam String responderUserId, @RequestParam SwapStatus swapStatus) {
    return ResponseEntity.ok(swapService.getReceivedRequests(responderUserId, swapStatus));
  }

  @GetMapping("/requests/for-book")
  public ResponseEntity<List<SwapResponse>> getRequestsForBook(
      @RequestParam String userId, @RequestParam String bookId) {
    return ResponseEntity.ok(swapService.getRequestsForBook(userId, bookId));
  }

  @PostMapping("/requests/create")
  public ResponseEntity<SwapResponse> createSwapRequest(@RequestBody CreateSwapDTO createSwapDTO) {
    return ResponseEntity.ok(swapService.createSwapRequest(createSwapDTO));
  }

  @PostMapping("/requests/cancel")
  public ResponseEntity<SwapResponse> cancelSwapRequest(@RequestBody CancelSwapDTO cancelSwapDTO) {
    return ResponseEntity.ok(swapService.cancelSwapRequest(cancelSwapDTO));
  }

  @PostMapping("/requests/accept")
  public ResponseEntity<SwapResponse> acceptSwapRequest(@RequestBody AcceptSwapDTO acceptSwapDTO) {
    return ResponseEntity.ok(swapService.acceptSwapRequest(acceptSwapDTO));
  }
}

package com.bookswap.swap_service.controller;

import com.bookswap.swap_service.domain.swap.SwapStatus;
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

  @PostMapping("/requests/sent")
  public ResponseEntity<List<SwapResponse>> getUserRequests(
      @RequestParam String requesterUserId, @RequestParam SwapStatus swapStatus) {
    return ResponseEntity.ok(swapService.getUserRequests(requesterUserId, swapStatus));
  }

  @PostMapping("/requests/create")
  public ResponseEntity<SwapResponse> createSwapRequest(@RequestBody CreateSwapDTO createSwapDTO) {
    return ResponseEntity.ok(swapService.createSwapRequest(createSwapDTO));
  }
}

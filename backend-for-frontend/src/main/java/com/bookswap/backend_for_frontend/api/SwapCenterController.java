package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.client.swap.dto.request.CreateSwapDto;
import com.bookswap.backend_for_frontend.client.swap.dto.response.SwapResponseDto;
import com.bookswap.backend_for_frontend.service.SwapCenterService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/swap")
@AllArgsConstructor
@Slf4j
public class SwapCenterController {
  private final SwapCenterService swapCenterService;

  @GetMapping("/me/sent")
  public ResponseEntity<List<SwapResponseDto>> getMySentSwapRequests(
      @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    return ResponseEntity.ok(swapCenterService.getMySentSwapRequests(userId));
  }

  @GetMapping("/me/received")
  public ResponseEntity<List<SwapResponseDto>> getMyReceivedSwapRequests(
      @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    return ResponseEntity.ok(swapCenterService.getMyReceivedSwapRequests(userId));
  }

  @GetMapping("/book/{bookId}/requests")
  public ResponseEntity<List<SwapResponseDto>> getSwapRequestsForBook(
      @PathVariable String bookId, @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    return ResponseEntity.ok(swapCenterService.getSwapRequestsForBook(userId, bookId));
  }

  @PostMapping("/cancel/{swapId}")
  public ResponseEntity<SwapResponseDto> cancelSwapRequest(
          @PathVariable String swapId,
          @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    return ResponseEntity.ok(swapCenterService.cancelSwapRequest(swapId, userId));
  }

  @PostMapping("/create")
  public ResponseEntity<SwapResponseDto> createSwapRequest(
      @RequestBody CreateSwapDto createSwapDto) {
    return ResponseEntity.ok(swapCenterService.createSwapRequest(createSwapDto));
  }

  @PostMapping("/accept/{swapId}")
  public ResponseEntity<SwapResponseDto> acceptSwapRequest(
          @PathVariable String swapId,
          @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    return ResponseEntity.ok(swapCenterService.acceptSwapRequest(swapId, userId));
  }

}

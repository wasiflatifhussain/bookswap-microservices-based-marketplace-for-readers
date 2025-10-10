package com.bookswap.wallet_service.controller;

import com.bookswap.wallet_service.dto.request.WalletMutationRequest;
import com.bookswap.wallet_service.dto.response.BalanceResponse;
import com.bookswap.wallet_service.dto.response.WalletMutationResponse;
import com.bookswap.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@AllArgsConstructor
@Slf4j
public class WalletController {
  private final WalletService walletService;

  @GetMapping("/me/balance")
  public ResponseEntity<BalanceResponse> getUserBalance(Authentication authentication) {
    return ResponseEntity.ok(walletService.getUserBalance(authentication.getName()));
  }

  @PostMapping("/{userId}/reserve")
  public ResponseEntity<WalletMutationResponse> reserveUserBalance(
      @PathVariable String userId,
      @Valid @RequestBody WalletMutationRequest walletMutationRequest) {
    return ResponseEntity.ok(walletService.reserveUserBalance(userId, walletMutationRequest));
  }

  @PostMapping("/{userId}/release")
  public ResponseEntity<WalletMutationResponse> releaseUserBalance(
      @PathVariable String userId,
      @Valid @RequestBody WalletMutationRequest walletMutationRequest) {
    return ResponseEntity.ok(walletService.releaseUserBalance(userId, walletMutationRequest));
  }

  @PostMapping("/{userId}/confirm")
  public ResponseEntity<WalletMutationResponse> confirmSwapSuccess(
      @PathVariable String userId,
      @Valid @RequestBody WalletMutationRequest walletMutationRequest) {
    return ResponseEntity.ok(walletService.confirmSwapSuccess(userId, walletMutationRequest));
  }
}

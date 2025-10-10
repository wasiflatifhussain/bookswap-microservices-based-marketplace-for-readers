package com.bookswap.wallet_service.controller;

import com.bookswap.wallet_service.dto.event.BookFinalizedEvent;
import com.bookswap.wallet_service.dto.event.BookUnlistedEvent;
import com.bookswap.wallet_service.dto.response.BalanceResponse;
import com.bookswap.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet/ops")
@AllArgsConstructor
@Slf4j
public class WalletOpsController {
  private final WalletService walletService;

  @PostMapping("/{userId}/add")
  public ResponseEntity<BalanceResponse> addToUserWallet(
      @PathVariable String userId, @Valid @RequestBody BookFinalizedEvent finalizedEvent) {
    return ResponseEntity.ok(walletService.addToUserWallet(userId, finalizedEvent));
  }

  @PostMapping("/{userId}/delete")
  public ResponseEntity<BalanceResponse> deleteFromUserWallet(
      @PathVariable String userId, @Valid @RequestBody BookUnlistedEvent unlistedEvent) {
    return ResponseEntity.ok(walletService.deleteFromUserWallet(userId, unlistedEvent));
  }
}

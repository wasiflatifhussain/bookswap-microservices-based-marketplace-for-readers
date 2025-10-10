package com.bookswap.wallet_service.controller;

import com.bookswap.wallet_service.dto.response.BalanceResponse;
import com.bookswap.wallet_service.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

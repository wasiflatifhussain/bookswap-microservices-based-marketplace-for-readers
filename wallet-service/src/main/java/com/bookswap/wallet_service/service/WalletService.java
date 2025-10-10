package com.bookswap.wallet_service.service;

import com.bookswap.wallet_service.domain.wallet.Wallet;
import com.bookswap.wallet_service.dto.event.BookFinalizedEvent;
import com.bookswap.wallet_service.dto.event.BookUnlistedEvent;
import com.bookswap.wallet_service.dto.response.BalanceResponse;
import com.bookswap.wallet_service.repository.WalletRepository;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class WalletService {
  private final WalletRepository walletRepository;

  public BalanceResponse getUserBalance(String userId) {
    log.info("Searching for user wallet with userId={}", userId);

    try {
      Optional<Wallet> walletOptional = walletRepository.findByUserId(userId);
      if (walletOptional.isEmpty()) {
        return mapToBalanceResponse(userId, 0F, 0F, "Add a book to gain balance");
      }

      Wallet wallet = walletOptional.get();

      log.info("Successfully found user wallet for userId={}", userId);

      return mapToBalanceResponse(
          userId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "User wallet balance found successfully");
    } catch (Exception e) {
      log.error("Failed to find wallet for userId={} with error e={}", userId, e.getMessage(), e);
      return mapToBalanceResponse(userId, 0F, 0F, "Error during wallet fetch e=" + e.getMessage());
    }
  }

  public BalanceResponse addToUserWallet(String userId, BookFinalizedEvent finalizedEvent) {
    log.info("Initiating balance addition to userId={}", userId);

    try {
      Optional<Wallet> walletOptional = walletRepository.findByUserId(userId);
      if (walletOptional.isEmpty()) {
        Wallet wallet =
            Wallet.builder()
                .userId(userId)
                .availableAmount(finalizedEvent.getValuation())
                .reservedAmount(0F)
                .build();
        walletRepository.save(wallet);
        log.info("Created new wallet for userId={}", userId);
        return mapToBalanceResponse(
            userId,
            wallet.getAvailableAmount(),
            wallet.getReservedAmount(),
            "Balanced added successfully");
      }

      Wallet wallet = walletOptional.get();
      wallet.setAvailableAmount(wallet.getAvailableAmount() + finalizedEvent.getValuation());
      walletRepository.save(wallet);
      log.info("Added valuation to existing wallet for userId={}", userId);
      return mapToBalanceResponse(
          userId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Balanced added successfully");

    } catch (Exception e) {
      log.error("Failed to add to wallet for userId={} with error e={}", userId, e.getMessage(), e);
      return mapToBalanceResponse(
          userId, 0F, 0F, "Error adding to user wallet with error e=" + e.getMessage());
    }
  }

  /**
   * NOTE: In below code, I used Transactional for practice. It enables flushing and atomicity, and
   * avoids re-saves; this can easily also be impl-ed without transactional and by querying the
   * request, modify it, and then using .save() *
   */
  @Transactional
  public BalanceResponse deleteFromUserWallet(
      String userId, @Valid BookUnlistedEvent unlistedEvent) {
    log.info("Initiating balance deletion from userId={}", userId);
    try {
      Optional<Wallet> walletOptional = walletRepository.findByUserId(userId);
      if (walletOptional.isEmpty()) {
        log.info("No wallet found for userId={}", userId);
        return mapToBalanceResponse(userId, 0F, 0F, "No wallet found for userId=" + userId);
      }

      Wallet wallet = walletOptional.get();
      if (wallet.getAvailableAmount() - unlistedEvent.getValuation() < 0) {
        log.info("Deletion amount is greater than available balance for userId={}", userId);
        return mapToBalanceResponse(
            userId,
            wallet.getAvailableAmount(),
            wallet.getReservedAmount(),
            "Available balance is less than deletion amount=" + unlistedEvent.getValuation());
      }
      wallet.setAvailableAmount(wallet.getAvailableAmount() - unlistedEvent.getValuation());

      log.info(
          "Successfully deduced balance for user with userId={} and current availableAmount={}",
          userId,
          wallet.getAvailableAmount());
      return mapToBalanceResponse(
          userId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Balanced deducted successfully");
    } catch (Exception e) {
      log.error(
          "Failed to delete from wallet for userId={} with error e={}", userId, e.getMessage(), e);
      throw e; // this is to throw the error and ensure rollback during failed transactions
    }
  }

  private BalanceResponse mapToBalanceResponse(
      String userId, Float availableAmount, Float reservedAmount, String message) {
    return BalanceResponse.builder()
        .userId(userId)
        .availableAmount(availableAmount)
        .reservedAmount(reservedAmount)
        .message(message)
        .build();
  }
}

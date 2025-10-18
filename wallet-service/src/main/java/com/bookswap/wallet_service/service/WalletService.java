package com.bookswap.wallet_service.service;

import com.bookswap.wallet_service.domain.wallet.Wallet;
import com.bookswap.wallet_service.domain.wallet.WalletReservation;
import com.bookswap.wallet_service.dto.MutationType;
import com.bookswap.wallet_service.dto.event.BookFinalizedEvent;
import com.bookswap.wallet_service.dto.event.BookUnlistedEvent;
import com.bookswap.wallet_service.dto.request.WalletMutationRequest;
import com.bookswap.wallet_service.dto.response.BalanceResponse;
import com.bookswap.wallet_service.dto.response.WalletMutationResponse;
import com.bookswap.wallet_service.repository.WalletRepository;
import com.bookswap.wallet_service.repository.WalletReservationRepository;
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
  private final WalletReservationRepository walletReservationRepository;

  @Transactional(readOnly = true)
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

  @Transactional
  public BalanceResponse addToUserWallet(String userId, BookFinalizedEvent finalizedEvent) {
    log.info("Initiating balance addition to userId={}", userId);

    try {
      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(userId);
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
      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(userId);
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

  @Transactional
  public WalletMutationResponse reserveUserBalance(
      String userId, @Valid WalletMutationRequest walletMutationRequest) {

    log.info("Initiating balance reservation for userId={}", userId);

    Float reqAmount = walletMutationRequest.getAmount();
    if (reqAmount == null || reqAmount.isNaN() || reqAmount.isInfinite() || reqAmount <= 0F) {
      log.info("Invalid amount provided for reservation for userId={}", userId);
      return mapToWalletMutationResponse(
          userId,
          walletMutationRequest.getBookId(),
          walletMutationRequest.getSwapId(),
          reqAmount,
          walletMutationRequest.getMutationType(),
          0F,
          0F,
          "Invalid amount provided for reservation for userId=" + userId);
    }

    // NOTE: we need to account for floating point precision issues when comparing floats
    final Float errorMargin = 0.0001F;

    try {
      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(userId);
      if (walletOptional.isEmpty()) {
        log.info("No books or available amount exist for user with userId={}", userId);
        return mapToWalletMutationResponse(
            userId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No books or available amount exist for user with userId=" + userId);
      }

      Wallet wallet = walletOptional.get();
      if (walletMutationRequest.getAmount() - wallet.getAvailableAmount() > errorMargin) {
        log.info(
            "Valuation of book to be reserved is greater than available amount for userId={}",
            userId);
        return mapToWalletMutationResponse(
            userId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            wallet.getAvailableAmount(),
            wallet.getReservedAmount(),
            "Valuation of book to be reserved is greater than available amount for userId="
                + userId);
      }

      float newAvailableAmount = wallet.getAvailableAmount() - walletMutationRequest.getAmount();
      float newReservedAmount = wallet.getReservedAmount() + walletMutationRequest.getAmount();
      if (newAvailableAmount < 0F && Math.abs(newAvailableAmount) < errorMargin) {
        newAvailableAmount = 0F;
      }

      wallet.setAvailableAmount(newAvailableAmount);
      wallet.setReservedAmount(newReservedAmount);

      WalletReservation walletReservation =
          WalletReservation.builder()
              .userId(userId)
              .bookId(walletMutationRequest.getBookId())
              .swapId(walletMutationRequest.getSwapId())
              .amount(walletMutationRequest.getAmount())
              .build();
      walletReservationRepository.save(walletReservation);

      log.info(
          "Successfully reserved amount for user with userId={} and current availableAmount={} and reservedAmount={}",
          userId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount());
      return mapToWalletMutationResponse(
          userId,
          walletMutationRequest.getBookId(),
          walletMutationRequest.getSwapId(),
          walletMutationRequest.getAmount(),
          walletMutationRequest.getMutationType(),
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Balanced reserved successfully");
    } catch (Exception e) {
      log.error(
          "Failed to reserve from wallet for userId={} with error e={}", userId, e.getMessage(), e);
      throw e;
    }
  }

  @Transactional
  public WalletMutationResponse releaseUserBalance(
      String userId, @Valid WalletMutationRequest walletMutationRequest) {
    log.info("Initiating balance release for userId={}", userId);

    final float errorMargin = 0.0001F;

    try {
      Optional<WalletReservation> walletReservationOptional =
          walletReservationRepository.findByUserIdAndBookId(
              userId, walletMutationRequest.getBookId());

      if (walletReservationOptional.isEmpty()) {
        log.info(
            "No book entry exist for bookId={} and userId={}",
            walletMutationRequest.getBookId(),
            userId);

        return mapToWalletMutationResponse(
            userId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No book entry exist for bookId="
                + walletMutationRequest.getBookId()
                + " userId="
                + userId);
      }

      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(userId);
      if (walletOptional.isEmpty()) {
        log.info("No books or available amount exist for user with userId={}", userId);
        return mapToWalletMutationResponse(
            userId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No books or available amount exist for user with userId=" + userId);
      }

      WalletReservation walletReservation = walletReservationOptional.get();
      Wallet wallet = walletOptional.get();

      if (wallet.getAvailableAmount() == null) wallet.setAvailableAmount(0F);
      if (wallet.getReservedAmount() == null) wallet.setReservedAmount(0F);

      Float releaseAmount = walletReservation.getAmount();
      if (releaseAmount == null || releaseAmount.isNaN() || releaseAmount.isInfinite()) {
        releaseAmount = 0F;
      }

      float newReservedAmount = wallet.getReservedAmount() - releaseAmount;
      if (newReservedAmount < 0F && Math.abs(newReservedAmount) < errorMargin) {
        newReservedAmount = 0F;
      }
      Float newAvailableAmount = wallet.getAvailableAmount() + releaseAmount;

      wallet.setAvailableAmount(newAvailableAmount);
      wallet.setReservedAmount(newReservedAmount);

      walletReservationRepository.delete(walletReservation);

      log.info(
          "Successfully released reserved amount for user with userId={} and current availableAmount={} and reservedAmount={}",
          userId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount());
      return mapToWalletMutationResponse(
          userId,
          walletMutationRequest.getBookId(),
          walletMutationRequest.getSwapId(),
          walletMutationRequest.getAmount(),
          walletMutationRequest.getMutationType(),
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Balanced released successfully");

    } catch (Exception e) {
      log.info(
          "Failed to release from wallet for userId={} with error e={}", userId, e.getMessage(), e);
      throw e;
    }
  }

  @Transactional
  public WalletMutationResponse confirmSwapSuccessForRequester(
      String requesterUserId, @Valid WalletMutationRequest walletMutationRequest) {
    log.info("Initiating swap confirmation for requesterUserId={}", requesterUserId);

    try {
      Optional<WalletReservation> walletReservationOptional =
          walletReservationRepository.findByUserIdAndBookId(
              requesterUserId, walletMutationRequest.getBookId());

      if (walletReservationOptional.isEmpty()) {
        log.info(
            "No book entry exist for bookId={} and requesterUserId={}",
            walletMutationRequest.getBookId(),
            requesterUserId);

        return mapToWalletMutationResponse(
            requesterUserId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No book entry exist for bookId="
                + walletMutationRequest.getBookId()
                + " requesterUserId="
                + requesterUserId);
      }

      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(requesterUserId);
      if (walletOptional.isEmpty()) {
        log.info(
            "No books or available amount exist for user with requesterUserId={}", requesterUserId);
        return mapToWalletMutationResponse(
            requesterUserId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No books or available amount exist for user with requesterUserId=" + requesterUserId);
      }

      WalletReservation walletReservation = walletReservationOptional.get();
      Wallet wallet = walletOptional.get();

      if (wallet.getReservedAmount() == null) wallet.setReservedAmount(0F);

      Float confirmAmount = walletReservation.getAmount();
      if (confirmAmount == null || confirmAmount.isNaN() || confirmAmount.isInfinite()) {
        confirmAmount = 0F;
      }

      float newReservedAmount = wallet.getReservedAmount() - confirmAmount;
      if (newReservedAmount < 0F) {
        newReservedAmount = 0F;
      }

      wallet.setReservedAmount(newReservedAmount);

      walletReservationRepository.delete(walletReservation);

      log.info(
          "Successfully confirmed swap and deducted reserved amount for user with requesterUserId={} and current availableAmount={} and reservedAmount={}",
          requesterUserId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount());
      return mapToWalletMutationResponse(
          requesterUserId,
          walletMutationRequest.getBookId(),
          walletMutationRequest.getSwapId(),
          walletMutationRequest.getAmount(),
          walletMutationRequest.getMutationType(),
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Swap confirmed and reserved amount deducted successfully");
    } catch (Exception e) {
      log.info(
          "Failed to confirm swap for wallet for requesterUserId={} with error e={}",
          requesterUserId,
          e.getMessage(),
          e);
      throw e;
    }
  }

  @Transactional
  public WalletMutationResponse confirmSwapSuccessForResponder(
      String responderUserId, @Valid WalletMutationRequest walletMutationRequest) {
    log.info("Initiating swap confirmation for responderUserId={}", responderUserId);

    try {

      Optional<Wallet> walletOptional = walletRepository.findByUserIdForUpdate(responderUserId);
      if (walletOptional.isEmpty()) {
        log.info(
            "No books or available amount exist for user with responderUserId={}", responderUserId);
        return mapToWalletMutationResponse(
            responderUserId,
            walletMutationRequest.getBookId(),
            walletMutationRequest.getSwapId(),
            walletMutationRequest.getAmount(),
            walletMutationRequest.getMutationType(),
            0F,
            0F,
            "No books or available amount exist for user with responderUserId=" + responderUserId);
      }

      Wallet wallet = walletOptional.get();

      if (wallet.getReservedAmount() == null) wallet.setReservedAmount(0F);

      float newAvailableAmount = wallet.getAvailableAmount() - walletMutationRequest.getAmount();
      if (newAvailableAmount < 0F) {
        newAvailableAmount = 0F;
      }

      wallet.setAvailableAmount(newAvailableAmount);

      log.info(
          "Successfully confirmed swap and deducted available amount for user with responderUserId={} and current availableAmount={} and reservedAmount={}",
          responderUserId,
          wallet.getAvailableAmount(),
          wallet.getReservedAmount());
      return mapToWalletMutationResponse(
          responderUserId,
          walletMutationRequest.getBookId(),
          walletMutationRequest.getSwapId(),
          walletMutationRequest.getAmount(),
          walletMutationRequest.getMutationType(),
          wallet.getAvailableAmount(),
          wallet.getReservedAmount(),
          "Swap confirmed and reserved amount deducted successfully");
    } catch (Exception e) {
      log.info(
          "Failed to confirm swap for wallet for responderUserId={} with error e={}",
          responderUserId,
          e.getMessage(),
          e);
      throw e;
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

  private WalletMutationResponse mapToWalletMutationResponse(
      String userId,
      String bookId,
      String swapId,
      Float amount,
      MutationType mutationType,
      Float availableAmount,
      Float reservedAmount,
      String message) {
    return WalletMutationResponse.builder()
        .userId(userId)
        .bookId(bookId)
        .swapId(swapId)
        .amount(amount)
        .mutationType(mutationType)
        .availableAmount(availableAmount)
        .reservedAmount(reservedAmount)
        .message(message)
        .build();
  }
}

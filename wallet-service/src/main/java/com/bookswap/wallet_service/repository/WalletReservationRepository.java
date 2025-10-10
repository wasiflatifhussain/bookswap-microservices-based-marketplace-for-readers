package com.bookswap.wallet_service.repository;

import com.bookswap.wallet_service.domain.wallet.WalletReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletReservationRepository extends JpaRepository<WalletReservation, String> {
  Optional<WalletReservation> findByUserIdAndBookId(String userId, String bookId);
}

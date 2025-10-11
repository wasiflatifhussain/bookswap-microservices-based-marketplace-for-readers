package com.bookswap.wallet_service.repository;

import com.bookswap.wallet_service.domain.wallet.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, String> {
  // Plain READ: for balance inquiry
  Optional<Wallet> findByUserId(String userId);

  // PESSIMISTIC WRITE LOCK: for operations that modify the wallet
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
  Optional<Wallet> findByUserIdForUpdate(@Param("userId") String userId);
}

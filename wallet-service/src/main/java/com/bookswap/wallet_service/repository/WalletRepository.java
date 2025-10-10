package com.bookswap.wallet_service.repository;

import com.bookswap.wallet_service.domain.wallet.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, String> {
  Optional<Wallet> findByUserId(String userId);
}

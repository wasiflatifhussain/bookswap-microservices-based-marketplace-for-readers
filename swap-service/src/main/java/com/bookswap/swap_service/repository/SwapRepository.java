package com.bookswap.swap_service.repository;

import com.bookswap.swap_service.domain.swap.Swap;
import com.bookswap.swap_service.domain.swap.SwapStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRepository extends JpaRepository<Swap, String> {
  List<Swap> findByRequesterUserIdAndSwapStatus(String requesterUserId, SwapStatus swapStatus);
}

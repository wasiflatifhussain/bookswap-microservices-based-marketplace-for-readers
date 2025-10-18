package com.bookswap.swap_service.repository;

import com.bookswap.swap_service.domain.swap.Swap;
import com.bookswap.swap_service.domain.swap.SwapStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwapRepository extends JpaRepository<Swap, String> {
  List<Swap> findByRequesterUserIdAndSwapStatus(String requesterUserId, SwapStatus swapStatus);

  List<Swap> findByResponderUserIdAndSwapStatus(String responderUserId, SwapStatus swapStatus);

  List<Swap> findByResponderUserIdAndResponderBookId(
      String responderUserId, String responderBookId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Swap s WHERE s.swapId = :swapId")
  Optional<Swap> findBySwapIdForUpdate(@Param("swapId") String swapId);

  @Query(
      """
    SELECT s FROM Swap s
    WHERE s.responderBookId = :responderBookId
    AND s.swapStatus = 'PENDING'
    AND s.swapId <> :excludeSwapId
    """)
  List<Swap> findPendingByResponderBookIdExcludingSwapId(
      String responderBookId, String excludeSwapId);
}

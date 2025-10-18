package com.bookswap.swap_service.dto.response;

import com.bookswap.swap_service.client.catalog.dto.BookResponseWithMedia;
import com.bookswap.swap_service.domain.swap.SwapStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapResponse {
  private String swapId;
  private String requesterUserId;
  private String responderUserId;
  private String requesterBookId;
  private String responderBookId;
  private SwapStatus swapStatus;
  private BookResponseWithMedia requesterBook;
  private BookResponseWithMedia responderBook;
  private String message;
}



//@Transactional
//public SwapResponse cancelSwapRequest(CancelSwapDTO cancelSwapDTO) {
//  log.info(
//          "Initiating cancel for swapId={} by user={}",
//          cancelSwapDTO.getSwapId(),
//          cancelSwapDTO.getRequesterUserId());
//
//  Swap swap =
//          swapRepository
//                  .findBySwapIdForUpdate(cancelSwapDTO.getSwapId())
//                  .orElseThrow(() -> new IllegalArgumentException("Swap not found"));
//
//  // State guard: only allow cancel when PENDING
//  if (swap.getSwapStatus() != SwapStatus.PENDING) {
//    throw new IllegalStateException("Only PENDING swaps can be cancelled");
//  }
//
//  // Only requester can cancel
//  if (cancelSwapDTO.getRequesterUserId() != null
//          && !Objects.equals(swap.getRequesterUserId(), cancelSwapDTO.getRequesterUserId())) {
//    throw new IllegalStateException("Only the requester can cancel this swap");
//  }
//
//  try {
//    // Best-effort release wallet (idempotent on Wallet side by (userId, swapId))
//    releaseWallet(
//            swap.getRequesterUserId(),
//            swap.getSwapId(),
//            swap.getRequesterBookId(),
//            swap.getRequesterBookPrice());
//
//    // Best-effort unreserve the requester's book (Catalog should be idempotent)
//    unreserveCatalog(swap.getRequesterBookId());
//
//    // Hard delete the swap row
//    deleteSwapBestEffort(swap);
//
//    // Notify others
//    publishSwapCancelledEvent(swap);
//
//    // Return a “deleted” style response (mirrors your failureResponseAfterDelete)
//    return SwapResponse.builder()
//            .swapId(swap.getSwapId())
//            .requesterUserId(swap.getRequesterUserId())
//            .responderUserId(swap.getResponderUserId())
//            .requesterBookId(swap.getRequesterBookId())
//            .responderBookId(swap.getResponderBookId())
//            .swapStatus(null)
//            .message("Swap cancelled and deleted successfully.")
//            .build();
//
//  } catch (Exception e) {
//    log.error("Error cancelling swapId={} err={}", cancelSwapDTO.getSwapId(), e.toString());
//    throw e;
//  }
//}
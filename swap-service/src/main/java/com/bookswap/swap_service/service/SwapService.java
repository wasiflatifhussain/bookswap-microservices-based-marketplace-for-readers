package com.bookswap.swap_service.service;

import com.bookswap.swap_service.client.catalog.CatalogServiceClient;
import com.bookswap.swap_service.client.catalog.dto.BookRequest;
import com.bookswap.swap_service.client.catalog.dto.BookResponseDetailed;
import com.bookswap.swap_service.client.catalog.dto.BookResponseWithMedia;
import com.bookswap.swap_service.client.wallet.WalletServiceClient;
import com.bookswap.swap_service.client.wallet.dto.WalletMutationRequest;
import com.bookswap.swap_service.client.wallet.dto.WalletMutationResponse;
import com.bookswap.swap_service.domain.outbox.AggregateType;
import com.bookswap.swap_service.domain.swap.Swap;
import com.bookswap.swap_service.domain.swap.SwapStatus;
import com.bookswap.swap_service.dto.event.SwapCancelEvent;
import com.bookswap.swap_service.dto.event.SwapCompletedEvent;
import com.bookswap.swap_service.dto.event.SwapCreatedEvent;
import com.bookswap.swap_service.dto.request.AcceptSwapDTO;
import com.bookswap.swap_service.dto.request.CancelSwapDTO;
import com.bookswap.swap_service.dto.request.CreateSwapDTO;
import com.bookswap.swap_service.dto.response.SwapResponse;
import com.bookswap.swap_service.repository.SwapRepository;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class SwapService {
  private final SwapRepository swapRepository;
  private final CatalogServiceClient catalogServiceClient;
  private final WalletServiceClient walletServiceClient;
  private final OutboxService outboxService;

  @Transactional(readOnly = true)
  public List<SwapResponse> getUserRequests(String requesterUserId, SwapStatus swapStatus) {
    log.info(
        "Initiating getting requests for requesterUserId={} with swapStatus={}",
        requesterUserId,
        swapStatus);

    try {
      List<Swap> swaps =
          swapRepository.findByRequesterUserIdAndSwapStatus(requesterUserId, swapStatus);
      if (swaps.isEmpty()) {
        log.info(
            "No requests found for user with requesterUserId={} and swapStatus={}",
            requesterUserId,
            swapStatus);
        return mapToSwapResponses(
            List.of(Swap.builder().requesterUserId(requesterUserId).swapStatus(swapStatus).build()),
            Map.of(),
            "No requests found for user with this requesterId and swapStatus");
      }

      Set<String> bookIds = collectBookIds(swaps);

      Map<String, BookResponseWithMedia> bookMap = makeBookMap(bookIds);

      return mapToSwapResponses(swaps, bookMap, "Book found successfully");

    } catch (Exception e) {
      log.error(
          "Error occurred while fetching requests for requesterUserId={} with swapStatus={} and error={}",
          requesterUserId,
          swapStatus,
          e.getMessage());
      return mapToSwapResponses(
          List.of(Swap.builder().requesterUserId(requesterUserId).swapStatus(swapStatus).build()),
          Map.of(),
          "Error occurred while processing the request");
    }
  }

  @Transactional(readOnly = true)
  public List<SwapResponse> getReceivedRequests(String responderUserId, SwapStatus swapStatus) {
    log.info(
        "Initiating getting received requests for responderUserId={} with swapStatus={}",
        responderUserId,
        swapStatus);

    try {
      List<Swap> swaps =
          swapRepository.findByResponderUserIdAndSwapStatus(responderUserId, swapStatus);
      if (swaps.isEmpty()) {
        log.info(
            "No requests found for user with responderUserId={} and swapStatus={}",
            responderUserId,
            swapStatus);
        return mapToSwapResponses(
            List.of(Swap.builder().responderUserId(responderUserId).swapStatus(swapStatus).build()),
            Map.of(),
            "No requests found for user with this responderUserId and swapStatus");
      }

      Set<String> bookIds = collectBookIds(swaps);

      Map<String, BookResponseWithMedia> bookMap = makeBookMap(bookIds);

      return mapToSwapResponses(swaps, bookMap, "Book found successfully");

    } catch (Exception e) {
      log.error(
          "Error occurred while fetching requests for responderUserId={} with swapStatus={} and error={}",
          responderUserId,
          swapStatus,
          e.getMessage());
      return mapToSwapResponses(
          List.of(Swap.builder().responderUserId(responderUserId).swapStatus(swapStatus).build()),
          Map.of(),
          "Error occurred while processing the request");
    }
  }

  @Transactional(readOnly = true)
  public List<SwapResponse> getRequestsForBook(String userId, String bookId) {
    log.info("Initiating getting requests for userId={} and bookId={}", userId, bookId);

    try {
      List<Swap> swaps = swapRepository.findByResponderUserIdAndResponderBookId(userId, bookId);
      if (swaps.isEmpty()) {
        log.info("No requests found for userId={} and bookId={}", userId, bookId);
        return mapToSwapResponses(
            List.of(Swap.builder().requesterUserId(userId).responderUserId(userId).build()),
            Map.of(),
            "No requests found for user with this userId and bookId");
      }

      Set<String> bookIds = collectBookIds(swaps);
      Map<String, BookResponseWithMedia> bookMap = makeBookMap(bookIds);
      return mapToSwapResponses(swaps, bookMap, "Book found successfully");

    } catch (Exception e) {
      log.error(
          "Error occurred while fetching requests for userId={} and bookId={} with error={}",
          userId,
          bookId,
          e.getMessage());
      return mapToSwapResponses(
          List.of(Swap.builder().requesterUserId(userId).responderUserId(userId).build()),
          Map.of(),
          "Error occurred while processing the request");
    }
  }

  /**
   * Create a swap request.
   *
   * <p>Implementation (overview): 1) Fetch & validate both books from Catalog (must be AVAILABLE).
   * 2) Reserve the requester's book in Catalog. 3) Persist the swap row (PENDING snapshot). 4)
   * Reserve requester funds in Wallet (idempotent by userId+swapId). On wallet failure, unreserve
   * catalog and delete the swap row (best-effort rollback). 5) Publish SWAP_CREATED event to the
   * outbox.
   *
   * <p>This method is transactional for local DB operations; external calls are compensated
   * explicitly on error.
   */
  @Transactional
  public SwapResponse createSwapRequest(CreateSwapDTO dto) {
    log.info("Initiating creation of swap request for createSwapDTO={}", dto);

    BookResponseDetailed requesterBook = null;
    BookResponseDetailed responderBook = null;
    Swap swap = null;
    boolean catalogReserved = false;
    boolean walletReserved = false;

    try {
      // Fetch & validate
      requesterBook = fetchAndValidateBook(dto.getRequesterBookId(), "Requester");
      responderBook = fetchAndValidateBook(dto.getResponderBookId(), "Responder");
      ensureBothAvailable(requesterBook, responderBook);

      // Reserve in Catalog
      reserveRequesterBookOrFail(requesterBook);
      catalogReserved = true;

      // Persist swap row
      swap = persistSwapRow(requesterBook, responderBook);

      // Reserve in Wallet (or rollback & return)
      walletReserved =
          reserveWalletOrRollbackAndDeleteIfNeeded(
              swap, requesterBook, responderBook, catalogReserved);
      if (!walletReserved) {
        // Already rolled back + deleted inside the helper; return failure response.
        return failureResponseAfterDelete(
            requesterBook,
            responderBook,
            "Swap creation failed: wallet reservation could not be completed. Rolled back.");
      }

      // Publish created event & return success
      publishSwapCreatedEvent(swap, requesterBook, responderBook);
      return buildSuccessResponse(swap, requesterBook, responderBook);

    } catch (Exception e) {
      log.error("Error creating swap entry for createSwapDTO={} with error={}", dto, e.toString());

      // Best-effort compensation on exception for any succeeded external steps
      if (walletReserved && requesterBook != null) {
        releaseWallet(
            requesterBook.getOwnerUserId(),
            swap != null ? swap.getSwapId() : null,
            requesterBook.getBookId(),
            requesterBook.getValuation());
      }
      if (catalogReserved && requesterBook != null) {
        unreserveCatalog(requesterBook.getBookId());
      }
      if (swap != null) {
        deleteSwapBestEffort(swap);
      }
      throw e;
    }
  }

  /**
   * Cancel a pending swap request.
   *
   * <p>Implementation (overview): 1) Lock the swap row (FOR UPDATE) and validate state (PENDING)
   * and requester identity (if provided). 2) Release wallet reservation (best-effort, idempotent)
   * and unreserve the requester book in Catalog. 3) Delete the swap row and publish a SWAP_CANCELED
   * event.
   */
  @Transactional
  public SwapResponse cancelSwapRequest(CancelSwapDTO cancelSwapDTO) {
    log.info(
        "Initiating cancel for swapId={} by user={}",
        cancelSwapDTO.getSwapId(),
        cancelSwapDTO.getRequesterUserId());

    Swap swap =
        swapRepository
            .findBySwapIdForUpdate(cancelSwapDTO.getSwapId())
            .orElseThrow(() -> new IllegalArgumentException("Swap not found"));

    // State guard: only allow cancel when PENDING
    if (swap.getSwapStatus() != SwapStatus.PENDING) {
      throw new IllegalStateException("Only PENDING swaps can be cancelled");
    }

    // Only requester can cancel
    if (cancelSwapDTO.getRequesterUserId() != null
        && !Objects.equals(swap.getRequesterUserId(), cancelSwapDTO.getRequesterUserId())) {
      throw new IllegalStateException("Only the requester can cancel this swap");
    }

    try {
      // Best-effort release wallet (idempotent on Wallet side by (userId, swapId))
      releaseWallet(
          swap.getRequesterUserId(),
          swap.getSwapId(),
          swap.getRequesterBookId(),
          swap.getRequesterBookPrice());

      // Best-effort unreserve the requester's book (Catalog should be idempotent)
      unreserveCatalog(swap.getRequesterBookId());

      // Hard delete the swap row
      deleteSwapBestEffort(swap);

      // Notify others
      publishSwapCancelledEvent(swap);

      // Return a “deleted” style response (mirrors your failureResponseAfterDelete)
      return SwapResponse.builder()
          .swapId(swap.getSwapId())
          .requesterUserId(swap.getRequesterUserId())
          .responderUserId(swap.getResponderUserId())
          .requesterBookId(swap.getRequesterBookId())
          .responderBookId(swap.getResponderBookId())
          .swapStatus(null)
          .message("Swap cancelled and deleted successfully.")
          .build();

    } catch (Exception e) {
      log.error("Error cancelling swapId={} err={}", cancelSwapDTO.getSwapId(), e.toString());
      throw e;
    }
  }

  /**
   * Accepts a pending swap.
   *
   * <p>Sequence (matches implementation): 1. Lock the swap row with a FOR UPDATE read to prevent
   * concurrent modifications. 2. Validate the responderUserId matches the swap. 3. Call Catalog
   * Service to confirm the trade (blocks up to 10s). 4. Confirm reserved funds with Wallet Service
   * for both requester and responder (each call blocks up to 10s and is validated by checking the
   * returned message contains "confirmed"). 5. Cancel all other pending requests for the responder
   * book: release requester wallet reservations, unreserve requester books in Catalog, delete those
   * swap rows, and publish cancellation events. 6. Delete the accepted swap row and enqueue a
   * SWAP_COMPLETED event to the outbox for notifications.
   *
   * <p>Notes: - External calls are not covered by the DB transaction; failures result in exceptions
   * and rely on best-effort compensations / idempotency of downstream services. - The method
   * deletes the swap row on completion rather than persisting an ACCEPTED/COMPLETED state.
   */
  @Transactional
  public SwapResponse acceptSwapRequest(AcceptSwapDTO acceptSwapDTO) {
    log.info(
        "Initiating accept for swapId={} by user={}",
        acceptSwapDTO.getSwapId(),
        acceptSwapDTO.getResponderUserId());

    try {
      Optional<Swap> swapOptional = swapRepository.findBySwapIdForUpdate(acceptSwapDTO.getSwapId());
      if (swapOptional.isEmpty()) {
        log.error("No swap object found with swapId={}", acceptSwapDTO.getSwapId());
        return SwapResponse.builder()
            .message("No swap object found with swapId=" + acceptSwapDTO.getSwapId())
            .build();
      }

      Swap swap = swapOptional.get();

      if (!Objects.equals(swap.getResponderUserId(), acceptSwapDTO.getResponderUserId())) {
        log.error(
            "Mismatch between swap responderUserId={} and request's responderUserId={}",
            swap.getResponderUserId(),
            acceptSwapDTO.getResponderUserId());
        return SwapResponse.builder()
            .message("Mismatch between swap responderUserId and request's responderUserId")
            .build();
      }

      Boolean catalogConfirmation =
          catalogServiceClient
              .confirmSwap(swap.getRequesterBookId(), swap.getResponderBookId())
              .block(Duration.ofSeconds(10));
      if (catalogConfirmation == null || !catalogConfirmation) {
        throw new IllegalStateException("Catalog confirmSwap failed");
      }

      WalletMutationResponse forRequesterUser =
          walletServiceClient
              .confirmSwapSuccessForRequester(
                  swap.getRequesterUserId(),
                  WalletMutationRequest.builder()
                      .bookId(swap.getRequesterBookId())
                      .swapId(swap.getSwapId())
                      .amount(swap.getRequesterBookPrice())
                      .mutationType("CONFIRMED")
                      .build())
              .block(Duration.ofSeconds(10));
      if (forRequesterUser == null
          || forRequesterUser.getMessage() == null
          || !forRequesterUser.getMessage().toLowerCase().contains("confirmed")) {
        throw new IllegalStateException("Wallet confirm (requester) failed");
      }

      WalletMutationResponse forResponderUser =
          walletServiceClient
              .confirmSwapSuccessForResponder(
                  swap.getResponderUserId(),
                  WalletMutationRequest.builder()
                      .bookId(swap.getResponderBookId())
                      .swapId(swap.getSwapId())
                      .amount(swap.getResponderBookPrice())
                      .mutationType("CONFIRMED")
                      .build())
              .block(Duration.ofSeconds(10));
      if (forResponderUser == null
          || forResponderUser.getMessage() == null
          || !forResponderUser.getMessage().toLowerCase().contains("confirmed")) {
        throw new IllegalStateException("Wallet confirm (responder) failed");
      }

      // Delete all requests in Swap DB which have responderBook as their responderBook since
      // responder book is no longer available
      // This is not required to be done for requesterBook since the system changes requesterBook
      // status to RESERVED upon request, preventing other's from sending request for this book

      cancelOtherPendingRequestsForResponderBook(swap);

      // Delete swapped book from Swap DB
      swapRepository.delete(swap);

      // Publish to email service so email serve gets the ids and send out emails to both parties
      publishSwapConfirmedEvent(swap);

      return SwapResponse.builder().message("Swap completed successfully.").build();
    } catch (Exception e) {
      throw e;
    }
  }

  private void cancelOtherPendingRequestsForResponderBook(Swap swap) {
    log.info(
        "Initiating cancellation of other pending requests for responderBookId={}",
        swap.getResponderBookId());

    List<Swap> swapsToCancel =
        swapRepository.findPendingByResponderBookIdExcludingSwapId(
            swap.getResponderBookId(), swap.getSwapId());

    for (Swap s : swapsToCancel) {
      try {
        // Best-effort release wallet (idempotent on Wallet side by (userId, swapId))
        releaseWallet(
            s.getRequesterUserId(),
            s.getSwapId(),
            s.getRequesterBookId(),
            s.getRequesterBookPrice());

        // Best-effort unreserve the requester's book (Catalog should be idempotent)
        unreserveCatalog(s.getRequesterBookId());

        // Hard delete the swap row
        deleteSwapBestEffort(s);

        // Notify others
        publishSwapCancelledEvent(s);
      } catch (Exception e) {
        log.error(
            "Error cancelling swapId={} for responderBookId={} err={}",
            s.getSwapId(),
            swap.getResponderBookId(),
            e.toString());
      }
    }
  }

  private BookResponseDetailed fetchAndValidateBook(String bookId, String role) {
    log.info("Fetching {} book from Catalog-Service for bookId={}", role, bookId);
    BookResponseDetailed book =
        catalogServiceClient.getBookByBookId(bookId).block(Duration.ofSeconds(5));

    if (book == null || book.getMessage() == null) {
      throw new IllegalArgumentException(role + " book fetch failed: null response/message");
    }
    if (book.getMessage().contains("Error while searching for book")
        || book.getMessage().contains("No books with this bookId exist")) {
      throw new IllegalArgumentException(role + " book error: " + book.getMessage());
    }
    if (book.getBookStatus() == null) {
      throw new IllegalStateException(role + " book has null status");
    }
    return book;
  }

  private void ensureBothAvailable(
      BookResponseDetailed requesterBook, BookResponseDetailed responderBook) {
    if (!"AVAILABLE".equals(requesterBook.getBookStatus())
        || !"AVAILABLE".equals(responderBook.getBookStatus())) {
      throw new IllegalStateException(
          "Failed to create swap request. One of the books in this transaction is not AVAILABLE.");
    }
  }

  private void reserveRequesterBookOrFail(BookResponseDetailed requesterBook) {
    Boolean ok =
        catalogServiceClient.reserveBook(requesterBook.getBookId()).block(Duration.ofSeconds(5));
    if (ok == null || !ok) {
      throw new IllegalStateException(
          "Failed to create swap request. Error: Failed to reserve requester's book.");
    }
    log.info("Successfully reserved requesterBook requesterBookId={}", requesterBook.getBookId());
  }

  private Swap persistSwapRow(
      BookResponseDetailed requesterBook, BookResponseDetailed responderBook) {
    Swap swap =
        Swap.builder()
            .requesterUserId(requesterBook.getOwnerUserId())
            .responderUserId(responderBook.getOwnerUserId())
            .requesterBookId(requesterBook.getBookId())
            .responderBookId(responderBook.getBookId())
            .swapStatus(
                SwapStatus.PENDING) // keep your current enum if you want it during success path
            .requesterBookPrice(requesterBook.getValuation())
            .responderBookPrice(responderBook.getValuation())
            .build();
    swap = swapRepository.save(swap);
    log.info("Swap row saved with swapId={}", swap.getSwapId());
    return swap;
  }

  /**
   * Attempts wallet reserve. If it fails, roll back Catalog (best-effort) and **DELETE the swap
   * row**. Returns true if wallet reservation succeeded; false otherwise.
   */
  private boolean reserveWalletOrRollbackAndDeleteIfNeeded(
      Swap swap,
      BookResponseDetailed requesterBook,
      BookResponseDetailed responderBook,
      boolean catalogReserved) {

    WalletMutationRequest walletMutationRequest =
        WalletMutationRequest.builder()
            .bookId(requesterBook.getBookId())
            .swapId(swap.getSwapId())
            .amount(requesterBook.getValuation())
            .mutationType("RESERVED")
            .build();

    WalletMutationResponse walletMutationResponse =
        walletServiceClient
            .reserveInRequestWallet(requesterBook.getOwnerUserId(), walletMutationRequest)
            .block(Duration.ofSeconds(5));

    boolean walletOk =
        walletMutationResponse != null
            && walletMutationResponse.getMessage() != null
            && walletMutationResponse.getMessage().toLowerCase().contains("reserved successfully");

    if (!walletOk) {
      log.warn(
          "Wallet reservation failed for swapId={}, msg={}",
          swap.getSwapId(),
          walletMutationResponse != null ? walletMutationResponse.getMessage() : "null response");

      if (catalogReserved) {
        unreserveCatalog(requesterBook.getBookId());
      }
      deleteSwapBestEffort(swap);
      return false;
    }
    return true;
  }

  private void publishSwapCreatedEvent(
      Swap swap, BookResponseDetailed requesterBook, BookResponseDetailed responderBook) {
    SwapCreatedEvent evt =
        SwapCreatedEvent.builder()
            .swapId(swap.getSwapId())
            .requesterUserId(requesterBook.getOwnerUserId())
            .requesterBookId(requesterBook.getBookId())
            .responderUserId(responderBook.getOwnerUserId())
            .responderBookId(responderBook.getBookId())
            .responderBookName(responderBook.getTitle())
            .build();

    outboxService.enqueueEvent(
        AggregateType.SWAP, responderBook.getOwnerUserId(), "SWAP_CREATED", evt);
  }

  private void publishSwapCancelledEvent(Swap swap) {
    SwapCancelEvent evt =
        SwapCancelEvent.builder()
            .swapId(swap.getSwapId())
            .requesterUserId(swap.getRequesterUserId())
            .requesterBookId(swap.getRequesterBookId())
            .responderUserId(swap.getResponderUserId())
            .responderBookId(swap.getResponderBookId())
            .build();

    outboxService.enqueueEvent(
        AggregateType.SWAP, swap.getResponderUserId(), "SWAP_CANCELLED", evt);
  }

  private void publishSwapConfirmedEvent(Swap swap) {
    SwapCompletedEvent evt =
        SwapCompletedEvent.builder()
            .swapId(swap.getSwapId())
            .requesterUserId(swap.getRequesterUserId())
            .requesterBookId(swap.getRequesterBookId())
            .responderUserId(swap.getResponderUserId())
            .responderBookId(swap.getResponderBookId())
            .build();

    outboxService.enqueueEvent(
        AggregateType.SWAP, swap.getResponderUserId(), "SWAP_COMPLETED", evt);
  }

  private SwapResponse buildSuccessResponse(
      Swap swap, BookResponseDetailed requesterBook, BookResponseDetailed responderBook) {
    return SwapResponse.builder()
        .swapId(swap.getSwapId())
        .requesterUserId(requesterBook.getOwnerUserId())
        .responderUserId(responderBook.getOwnerUserId())
        .requesterBookId(requesterBook.getBookId())
        .responderBookId(responderBook.getBookId())
        .swapStatus(swap.getSwapStatus())
        .requesterBook(null)
        .responderBook(null)
        .message("Swap Requested created successfully.")
        .build();
  }

  private SwapResponse failureResponseAfterDelete(
      BookResponseDetailed requesterBook, BookResponseDetailed responderBook, String message) {
    // swap row is deleted already; return a response without swap status/row
    return SwapResponse.builder()
        .swapId(null)
        .requesterUserId(requesterBook.getOwnerUserId())
        .responderUserId(responderBook.getOwnerUserId())
        .requesterBookId(requesterBook.getBookId())
        .responderBookId(responderBook.getBookId())
        .swapStatus(null)
        .message(message)
        .build();
  }

  private void unreserveCatalog(String bookId) {
    try {
      Boolean ok = catalogServiceClient.unreserveBook(bookId).block(Duration.ofSeconds(5));
      if (ok == null || !ok) {
        log.warn("Catalog unreserve returned false/null for bookId={}", bookId);
      } else {
        log.info("Catalog unreserve succeeded for bookId={}", bookId);
      }
    } catch (Exception ex) {
      log.warn("Catalog unreserve threw exception for bookId={}, err={}", bookId, ex.toString());
    }
  }

  private void releaseWallet(String userId, String swapId, String bookId, Float amount) {
    try {
      WalletMutationResponse rel =
          walletServiceClient
              .releaseReservedInRequestWallet(
                  userId,
                  WalletMutationRequest.builder()
                      .swapId(swapId)
                      .bookId(bookId)
                      .amount(amount)
                      .build())
              .block(Duration.ofSeconds(5));

      boolean responseStatus =
          rel != null
              && rel.getMessage() != null
              && rel.getMessage().toLowerCase().contains("released successfully");
      if (!responseStatus) {
        log.warn("Wallet release returned false/null for userId={}, swapId={}", userId, swapId);
      } else {
        log.info("Wallet release succeeded for userId={}, swapId={}", userId, swapId);
      }
    } catch (Exception ex) {
      log.warn("Wallet release threw exception for userId={}, err={}", userId, ex.toString());
    }
  }

  private void deleteSwapBestEffort(Swap swap) {
    try {
      swapRepository.delete(swap);
      log.info("Deleted swap row swapId={} (best-effort)", swap.getSwapId());
    } catch (Exception ex) {
      log.warn("Failed to delete swap row swapId={}, err={}", swap.getSwapId(), ex.toString());
    }
  }

  private Set<String> collectBookIds(List<Swap> swaps) {
    return swaps.stream()
        .flatMap(s -> Stream.of(s.getRequesterBookId(), s.getResponderBookId()))
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<String, BookResponseWithMedia> makeBookMap(Set<String> bookIds) {
    if (bookIds.isEmpty()) return Map.of();

    List<BookResponseWithMedia> list =
        catalogServiceClient
            .getBooksByBulkBookIdOrder(
                BookRequest.builder().bookIds(new ArrayList<>(bookIds)).build())
            .block(Duration.ofSeconds(15));

    if (list == null) list = List.of();

    return list.stream()
        .filter(b -> b.getBookId() != null)
        .collect(
            Collectors.toMap(BookResponseWithMedia::getBookId, Function.identity(), (a, b) -> a));
  }

  private List<SwapResponse> mapToSwapResponses(
      List<Swap> swaps, Map<String, BookResponseWithMedia> bookMap, String message) {
    return swaps.stream()
        .map(
            s ->
                SwapResponse.builder()
                    .swapId(s.getSwapId())
                    .requesterUserId(s.getRequesterUserId())
                    .responderUserId(s.getResponderUserId())
                    .requesterBookId(s.getRequesterBookId())
                    .responderBookId(s.getResponderBookId())
                    .swapStatus(s.getSwapStatus())
                    .requesterBook(bookMap.get(s.getRequesterBookId()))
                    .responderBook(bookMap.get(s.getResponderBookId()))
                    .message(message)
                    .build())
        .toList();
  }
}

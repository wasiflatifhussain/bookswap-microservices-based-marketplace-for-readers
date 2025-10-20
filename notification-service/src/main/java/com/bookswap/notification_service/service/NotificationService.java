package com.bookswap.notification_service.service;

import com.bookswap.notification_service.domain.BookSnapshot;
import com.bookswap.notification_service.domain.Notification;
import com.bookswap.notification_service.domain.NotificationType;
import com.bookswap.notification_service.domain.ReadStatus;
import com.bookswap.notification_service.dto.event.SwapCancelEvent;
import com.bookswap.notification_service.dto.event.SwapCompletedEvent;
import com.bookswap.notification_service.dto.event.SwapCreatedEvent;
import com.bookswap.notification_service.repository.BookSnapshotRepository;
import com.bookswap.notification_service.repository.NotificationRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final BookSnapshotRepository bookSnapshotRepository;

  @Transactional
  public void addNotificationToResponder(SwapCreatedEvent swapCreatedEvent) {
    log.info(
        "Initializing swap create notification for responder with swapId={}",
        swapCreatedEvent.getSwapId());

    try {
      Optional<BookSnapshot> bookSnapshotOpt1 =
          bookSnapshotRepository.findByBookId(swapCreatedEvent.getRequesterBookId());
      Optional<BookSnapshot> bookSnapshotOpt2 =
          bookSnapshotRepository.findByBookId(swapCreatedEvent.getResponderBookId());

      if (bookSnapshotOpt1.isEmpty() || bookSnapshotOpt2.isEmpty()) {
        throw new IllegalStateException("snapshot(s) missing");
      }

      BookSnapshot requesterBookSnapshot = bookSnapshotOpt1.get();
      BookSnapshot responderBookSnapshot = bookSnapshotOpt2.get();

      Notification notification =
          Notification.builder()
              .userId(responderBookSnapshot.getOwnerUserId())
              .notificationType(NotificationType.SWAP_CREATED)
              .title("New Book Swap Request")
              .readStatus(ReadStatus.UNREAD)
              .description(
                  "Swap Request for your book: "
                      + responderBookSnapshot.getTitle()
                      + ", valuation: "
                      + responderBookSnapshot.getValuation()
                      + " bookcoins, by user: "
                      + requesterBookSnapshot.getOwnerUserId()
                      + " in exchange for their book: "
                      + requesterBookSnapshot.getTitle()
                      + " of valuation: "
                      + requesterBookSnapshot.getValuation()
                      + " bookcoins. Go to Requests Center for more info.")
              .build();
      notificationRepository.save(notification);
      log.info(
          "Swap create notification saved for responderUserId={} for swapId={}",
          responderBookSnapshot.getOwnerUserId(),
          swapCreatedEvent.getSwapId());
    } catch (Exception e) {
      log.error("Failed to create notification for swapId={}", swapCreatedEvent.getSwapId(), e);
      throw new RuntimeException("Notification creation failed", e);
    }
  }

  @Transactional
  public void addNotificationToRequester(SwapCancelEvent swapCancelEvent) {
    log.info(
        "Initializing swap cancel notification for requester with swapId={}",
        swapCancelEvent.getSwapId());

    try {
      Optional<BookSnapshot> bookSnapshotOpt1 =
          bookSnapshotRepository.findByBookId(swapCancelEvent.getRequesterBookId());
      Optional<BookSnapshot> bookSnapshotOpt2 =
          bookSnapshotRepository.findByBookId(swapCancelEvent.getResponderBookId());

      if (bookSnapshotOpt1.isEmpty() || bookSnapshotOpt2.isEmpty()) {
        throw new IllegalStateException("snapshot(s) missing");
      }

      BookSnapshot requesterBookSnapshot = bookSnapshotOpt1.get();
      BookSnapshot responderBookSnapshot = bookSnapshotOpt2.get();

      Notification notification =
          Notification.builder()
              .userId(requesterBookSnapshot.getOwnerUserId())
              .notificationType(NotificationType.SWAP_CANCELLED)
              .title("Swap Request Cancelled")
              .readStatus(ReadStatus.UNREAD)
              .description(
                  "Swap Request cancelled for your book: "
                      + requesterBookSnapshot.getTitle()
                      + ", valuation: "
                      + requesterBookSnapshot.getValuation()
                      + " bookcoins. This request was sent to user: "
                      + responderBookSnapshot.getOwnerUserId()
                      + " for their book: "
                      + responderBookSnapshot.getTitle()
                      + " of valuation: "
                      + responderBookSnapshot.getValuation()
                      + " bookcoins."
                      + "Reason: You may have deleted the request, or the responder may have accepted another book request.")
              .build();
      notificationRepository.save(notification);
      log.info(
          "Swap cancel notification saved for requesterUserId={} for swapId={}",
          requesterBookSnapshot.getOwnerUserId(),
          swapCancelEvent.getSwapId());
    } catch (Exception e) {
      log.error("Failed to create notification for swapId={}", swapCancelEvent.getSwapId(), e);
      throw new RuntimeException("Notification creation failed", e);
    }
  }

  @Transactional
  public void addNotificationToBothUsers(SwapCompletedEvent swapCompletedEvent) {
    log.info(
        "Initializing swap completion notification for requester and responder with swapId={}",
        swapCompletedEvent.getSwapId());

    try {
      Optional<BookSnapshot> bookSnapshotOpt1 =
          bookSnapshotRepository.findByBookId(swapCompletedEvent.getRequesterBookId());
      Optional<BookSnapshot> bookSnapshotOpt2 =
          bookSnapshotRepository.findByBookId(swapCompletedEvent.getResponderBookId());

      if (bookSnapshotOpt1.isEmpty() || bookSnapshotOpt2.isEmpty()) {
        throw new IllegalStateException("snapshot(s) missing");
      }

      BookSnapshot requesterBookSnapshot = bookSnapshotOpt1.get();
      BookSnapshot responderBookSnapshot = bookSnapshotOpt2.get();

      Notification requesterNotification =
          Notification.builder()
              .userId(requesterBookSnapshot.getOwnerUserId())
              .notificationType(NotificationType.SWAP_COMPLETED)
              .title("Swap Successful!")
              .readStatus(ReadStatus.UNREAD)
              .description(
                  "Swap successful for your book: "
                      + requesterBookSnapshot.getTitle()
                      + ", valuation: "
                      + requesterBookSnapshot.getValuation()
                      + " bookcoins. This request was sent to user: "
                      + responderBookSnapshot.getOwnerUserId()
                      + " for their book: "
                      + responderBookSnapshot.getTitle()
                      + " of valuation: "
                      + responderBookSnapshot.getValuation()
                      + " bookcoins."
                      + "Please wait for email notification regarding trade details.")
              .build();

      Notification responderNotification =
          Notification.builder()
              .userId(responderBookSnapshot.getOwnerUserId())
              .notificationType(NotificationType.SWAP_COMPLETED)
              .title("Swap Successful!")
              .readStatus(ReadStatus.UNREAD)
              .description(
                  "Swap successful for your book: "
                      + responderBookSnapshot.getTitle()
                      + ", valuation: "
                      + responderBookSnapshot.getValuation()
                      + " bookcoins. "
                      + "This request was sent by user: "
                      + requesterBookSnapshot.getOwnerUserId()
                      + " for their book: "
                      + requesterBookSnapshot.getTitle()
                      + " of valuation: "
                      + requesterBookSnapshot.getValuation()
                      + " bookcoins. "
                      + "Please wait for email notification regarding trade details.")
              .build();
      notificationRepository.save(requesterNotification);
      log.info(
          "Swap completion notification saved for requesterUserId={} for swapId={}",
          requesterBookSnapshot.getOwnerUserId(),
          swapCompletedEvent.getSwapId());

      notificationRepository.save(responderNotification);
      log.info(
          "Swap completion notification saved for responderUserId={} for swapId={}",
          responderBookSnapshot.getOwnerUserId(),
          swapCompletedEvent.getSwapId());
    } catch (Exception e) {
      log.error("Failed to create notification for swapId={}", swapCompletedEvent.getSwapId(), e);
      throw new RuntimeException("Notification creation failed", e);
    }
  }
}

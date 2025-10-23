package com.bookswap.notification_service.service;

import com.bookswap.notification_service.domain.BookSnapshot;
import com.bookswap.notification_service.domain.Notification;
import com.bookswap.notification_service.domain.NotificationType;
import com.bookswap.notification_service.domain.ReadStatus;
import com.bookswap.notification_service.dto.event.SwapCancelEvent;
import com.bookswap.notification_service.dto.event.SwapCompletedEvent;
import com.bookswap.notification_service.dto.event.SwapCreatedEvent;
import com.bookswap.notification_service.dto.response.NotificationItem;
import com.bookswap.notification_service.repository.BookSnapshotRepository;
import com.bookswap.notification_service.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final BookSnapshotRepository bookSnapshotRepository;
  private final UnreadCounterService unreadCounterService;

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

      // NOTE: Increment unread counter only after transaction commit
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              unreadCounterService.increment(responderBookSnapshot.getOwnerUserId(), 1);
            }
          });
      log.info(
          "Unread counter incremented for responderUserId={} for swapId={}",
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
                      + " Reason: You may have deleted the request, or the responder may have accepted another book request.")
              .build();
      notificationRepository.save(notification);
      log.info(
          "Swap cancel notification saved for requesterUserId={} for swapId={}",
          requesterBookSnapshot.getOwnerUserId(),
          swapCancelEvent.getSwapId());

      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              unreadCounterService.increment(requesterBookSnapshot.getOwnerUserId(), 1);
            }
          });

      log.info(
          "Unread counter incremented for requesterUserId={} for swapId={}",
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
                      + " Please wait for email notification regarding trade details.")
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
                      + " bookcoins."
                      + " Please wait for email notification regarding trade details.")
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

      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              unreadCounterService.increment(requesterBookSnapshot.getOwnerUserId(), 1);
              unreadCounterService.increment(responderBookSnapshot.getOwnerUserId(), 1);
            }
          });
      log.info(
          "Unread counter incremented for requesterUserId={} and responderUserId={} for swapId={}",
          requesterBookSnapshot.getOwnerUserId(),
          responderBookSnapshot.getOwnerUserId(),
          swapCompletedEvent.getSwapId());

    } catch (Exception e) {
      log.error("Failed to create notification for swapId={}", swapCompletedEvent.getSwapId(), e);
      throw new RuntimeException("Notification creation failed", e);
    }
  }

  @Transactional(readOnly = true)
  public List<NotificationItem> getNotifications(
      String userId, boolean unreadOnly, int page, int size) {
    log.info(
        "Initializing getting notifications with unreadOnly={} page={} size={}",
        unreadOnly,
        page,
        size);

    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<Notification> entitiesFound =
          unreadOnly
              ? notificationRepository.findByUserIdAndReadStatus(
                  userId, ReadStatus.UNREAD, pageable)
              : notificationRepository.findByUserId(userId, pageable);

      return entitiesFound.stream()
          .map(
              notification ->
                  NotificationItem.builder()
                      .notificationId(notification.getNotificationId())
                      .userId(notification.getUserId())
                      .notificationType(notification.getNotificationType().name())
                      .title(notification.getTitle())
                      .description(notification.getDescription())
                      .readStatus(notification.getReadStatus().toString())
                      .build())
          .toList();
    } catch (Exception e) {
      log.error("Failed to get notifications", e);
      throw new RuntimeException("Get notifications failed", e);
    }
  }

  @Transactional
  public void markNotificationsAsRead(String userId, List<String> notificationIds) {
    log.info(
        "Initializing notification read for userId={} and notificationIds={}",
        userId,
        notificationIds);

    if (notificationIds == null || notificationIds.isEmpty()) return;

    try {
      // TODO: Implement retry mechanism for failed updates
      // TODO: Break the request to repo layer into 100 entries per request and add synchronization

      int updated = notificationRepository.markReadInBulk(userId, notificationIds);
      log.info("Marked {} notifications as read for userId={}", updated, userId);

      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              unreadCounterService.decrement(userId, updated);
            }
          });
      log.info("Unread counter decremented for userId={} by number={}", userId, updated);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Transactional(readOnly = true)
  public Integer getUnreadNotificationCount(String userId) {
    log.info("Initializing getting unread notification count for userId={}", userId);

    Integer unreadCount = unreadCounterService.getIfPresent(userId);

    // Cache Hit
    if (unreadCount != null) {
      log.info(
          "Unread notification cache hit for userId={} with unreadCount={}", userId, unreadCount);
      return unreadCount;
    }

    // Cache Miss - Query DB and populate cache
    log.info("Unread notification cache miss for userId={}", userId);
    try {
      int dbUnreadCount =
          notificationRepository.countByUserIdAndReadStatus(userId, ReadStatus.UNREAD);
      unreadCounterService.reset(userId, dbUnreadCount);
      log.info(
          "Unread notification count retrieved from DB and cache updated for userId={} with unreadCount={}",
          userId,
          dbUnreadCount);
      return dbUnreadCount;
    } catch (Exception e) {
      log.error("Failed to get unread notification count for userId={}", userId, e);
      throw new RuntimeException(e);
    }
  }
}

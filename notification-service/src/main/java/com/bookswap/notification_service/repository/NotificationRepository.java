package com.bookswap.notification_service.repository;

import com.bookswap.notification_service.domain.Notification;
import com.bookswap.notification_service.domain.ReadStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, String> {
  Page<Notification> findByUserId(String userId, Pageable pageable);

  Page<Notification> findByUserIdAndReadStatus(
      String userId, ReadStatus readStatus, Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
        UPDATE Notification n
        SET n.readStatus = 'READ'
        WHERE n.userId = :userId
        AND n.readStatus = 'UNREAD'
        AND n.notificationId IN :notificationIds
        """)
  int markReadInBulk(
      @Param("userId") String userId, @Param("notificationIds") List<String> notificationIds);

  int countByUserIdAndReadStatus(String userId, ReadStatus readStatus);
}

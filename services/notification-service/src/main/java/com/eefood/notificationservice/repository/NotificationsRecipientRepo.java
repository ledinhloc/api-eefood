package com.eefood.notificationservice.repository;

import com.eefood.notificationservice.model.NotificationsRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationsRecipientRepo extends JpaRepository<NotificationsRecipient, Long>{
    @Query("""
    SELECT nr FROM NotificationsRecipient nr
    JOIN FETCH nr.notification n
    WHERE n.isDeleted = false
      AND (nr.userId = :userId)
    ORDER BY n.createdAt DESC""")
    Page<NotificationsRecipient> findUserNotifications(Long userId, Pageable pageable);
    Optional<NotificationsRecipient> findByUserIdAndNotificationId(Long userId, Long notificationId);
    List<NotificationsRecipient> findByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
    Long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
}

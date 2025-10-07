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
    Page<NotificationsRecipient> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
    Optional<NotificationsRecipient> findByUserIdAndNotificationIdAndIsDeletedFalse(Long userId, Long notificationId);
    List<NotificationsRecipient> findByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
    List<NotificationsRecipient> findByUserIdAndIsDeletedFalse(Long userId);
    Long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
}

package com.eefood.notificationservice.repository;

import com.eefood.notificationservice.enums.NotificationsType;
import com.eefood.notificationservice.model.Notifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationsRepository extends JpaRepository<Notifications, Long>{

    @Query("""
    SELECT n FROM Notifications n WHERE n.isDeleted = false AND n.type = :type
    AND NOT EXISTS (
        SELECT nr FROM NotificationsRecipient nr
        WHERE nr.notification = n
          AND nr.userId = :userId
          AND nr.isDeleted = true
    )
    ORDER BY n.createdAt DESC
    """)
    Page<Notifications> findSystemNotificationsForUser(NotificationsType type, Long userId, Pageable pageable);

    @Query("""
    SELECT n FROM Notifications n WHERE n.isDeleted = false AND n.type = :type
    AND NOT EXISTS (
        SELECT nr FROM NotificationsRecipient nr
        WHERE nr.notification = n
          AND nr.userId = :userId
          AND (nr.isRead = true OR nr.isDeleted = true)) ORDER BY n.createdAt DESC
    """)
    List<Notifications> findUnreadSystemNotificationsForUser(NotificationsType type, Long userId);
}

package com.eefood.notificationservice.repository;

import com.eefood.notificationservice.enums.NotificationsType;
import com.eefood.notificationservice.model.NotificationsSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationsSettingRepository extends JpaRepository<NotificationsSetting, Long> {
    Optional<NotificationsSetting> findByUserIdAndType(Long userId, NotificationsType type);
    List<NotificationsSetting> findByUserId(Long userId);
}

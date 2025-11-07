package com.eefood.notificationservice.repository;

import com.eefood.notificationservice.model.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {
    Optional<UserFcmToken> findById(Long userId);
}

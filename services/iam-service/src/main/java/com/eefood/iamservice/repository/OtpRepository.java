package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.Otp;
import com.eefood.iamservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserAndOtpNumAndIsDeletedFalse(User user, String otpCode);
    int countByUserAndCreatedAtAfter(User user, LocalDateTime after); // dùng cho rate-limit
    Optional<Otp> findTopByUserOrderByCreatedAtDesc(User user); // lấy OTP mới nhất
}

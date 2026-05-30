package com.eefood.reactionservice.model.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long balanceDiamond = 0L;

    // Tổng số diamond đã nạp từ trước tới nay (không giảm)
    @Column(nullable = false)
    private Long totalTopup = 0L;

    // Tổng số diamond đã tiêu từ trước tới nay (không giảm)
    @Column(nullable = false)
    private Long totalSpent = 0L;
}

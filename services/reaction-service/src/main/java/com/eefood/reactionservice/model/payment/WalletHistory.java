package com.eefood.reactionservice.model.payment;

import com.eefood.reactionservice.enums.WalletHistoryType;
import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "wallet_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WalletHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WalletHistoryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceBefore;

    @Column(nullable = false)
    private Long balanceAfter;
}

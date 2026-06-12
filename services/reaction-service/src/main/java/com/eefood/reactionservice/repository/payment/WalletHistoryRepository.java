package com.eefood.reactionservice.repository.payment;

import com.eefood.reactionservice.enums.WalletHistoryType;
import com.eefood.reactionservice.model.payment.WalletHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {
    Page<WalletHistory> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") WalletHistoryType type,
            Pageable pageable
    );
    Page<WalletHistory> findByUserId(
            Long userId,
            Pageable pageable
    );
}

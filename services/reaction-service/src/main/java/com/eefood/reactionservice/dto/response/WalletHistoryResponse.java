package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.WalletHistoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletHistoryResponse {
    private Long id;
    private Long userId;
    private Long transactionId;
    private WalletHistoryType type;
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private LocalDateTime createdAt;
}

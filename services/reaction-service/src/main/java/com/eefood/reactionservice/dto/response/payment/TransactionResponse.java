package com.eefood.reactionservice.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private Long userId;
    private DiamondPackageResponse diamondPackage;
    private String status;
    private String provider;
    private String paymentUrl;
    private LocalDateTime createdAt;
}

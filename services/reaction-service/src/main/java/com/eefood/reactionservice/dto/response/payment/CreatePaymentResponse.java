package com.eefood.reactionservice.dto.response.payment;

import com.eefood.reactionservice.enums.TransactionProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentResponse {
    private Long transactionId;
    private TransactionProvider provider;
    private String paymentUrl;
    private String status;
}

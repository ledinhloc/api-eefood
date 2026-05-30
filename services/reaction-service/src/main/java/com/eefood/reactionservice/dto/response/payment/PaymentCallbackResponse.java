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
public class PaymentCallbackResponse {
    private Long transactionId;
    TransactionProvider provider;
    String status;
    String message;
}

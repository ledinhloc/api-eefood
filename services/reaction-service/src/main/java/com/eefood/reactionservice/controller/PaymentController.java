package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.CreatePaymentRequest;
import com.eefood.reactionservice.dto.response.payment.CreatePaymentResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.payment.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final VnPayService vnPayService;

    @PostMapping("/create")
    public ResponseData<CreatePaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = vnPayService.createPayment(request);
        return new ResponseData<>(HttpStatus.OK.value(), "Create payment successfully", response);
    }
}

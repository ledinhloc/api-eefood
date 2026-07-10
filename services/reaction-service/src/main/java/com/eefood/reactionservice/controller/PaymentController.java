package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.CreatePaymentRequest;
import com.eefood.reactionservice.dto.response.payment.CreatePaymentResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.payment.DiamondPackageResponse;
import com.eefood.reactionservice.service.payment.DiamondWalletService;
import com.eefood.reactionservice.service.payment.VnPayService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final VnPayService vnPayService;
    private final DiamondWalletService diamondWalletService;
    @GetMapping("/get-package")
    public ResponseData<List<DiamondPackageResponse>> getListDiamondPackages() {
        List<DiamondPackageResponse> responses = diamondWalletService.getAllPackages();
        return new ResponseData<>(HttpStatus.OK.value(), "Create payment successfully", responses);
    }

    @PostMapping("/create")
    public ResponseData<CreatePaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = vnPayService.createPayment(request);
        return new ResponseData<>(HttpStatus.OK.value(), "Create payment successfully", response);
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: {}", params);
        Map<String, String> result = vnPayService.processIPN(params);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<String> vnpayCallback(
            @RequestParam Map<String, String> params) {

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "99");
        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        String amount = params.getOrDefault("vnp_Amount", "0");

        boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);

        String deepLink = String.format(
                "eefood://eefood.app/payment/result?success=%s&txnRef=%s&amount=%s&responseCode=%s",
                isSuccess, txnRef, amount, responseCode
        );

        // Trả HTML tự redirect — WebView sẽ bắt được qua onNavigationRequest
        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<script>window.location.href='" + deepLink + "';</script>" +
                "</head><body>Đang quay lại ứng dụng...</body></html>";

        log.info("VNPay callback: responseCode={}, transactionStatus={}, isSuccess={}",
                responseCode, transactionStatus, isSuccess);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(html);
    }
}

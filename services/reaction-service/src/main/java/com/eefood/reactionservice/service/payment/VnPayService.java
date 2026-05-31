package com.eefood.reactionservice.service.payment;

import com.eefood.reactionservice.config.VnPayConfig;
import com.eefood.reactionservice.dto.response.payment.VNPayResponseCode;
import com.eefood.reactionservice.dto.request.CreatePaymentRequest;
import com.eefood.reactionservice.dto.response.payment.CreatePaymentResponse;
import com.eefood.reactionservice.dto.response.payment.PaymentCallbackResponse;
import com.eefood.reactionservice.dto.request.VnPayCallbackParams;
import com.eefood.reactionservice.enums.TransactionProvider;
import com.eefood.reactionservice.enums.TransactionStatus;
import com.eefood.reactionservice.model.payment.DiamondPackage;
import com.eefood.reactionservice.model.payment.Transaction;
import com.eefood.reactionservice.repository.DiamondPackageRepository;
import com.eefood.reactionservice.repository.TransactionRepository;
import com.eefood.reactionservice.util.SecurityUtil;
import com.eefood.reactionservice.util.VnPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayService {
    private final VnPayConfig vnPayConfig;
    private final TransactionRepository transactionRepository;
    private final DiamondPackageRepository diamondPackageRepository;
    private final DiamondWalletService diamondWalletService;
    private final SecurityUtil securityUtil;

    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        DiamondPackage pkg = diamondPackageRepository
                .findByIdAndIsActiveTrue(request.getDiamondPackageId())
                .orElseThrow(() -> new RuntimeException("Diamond package not found or inactive"));

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .diamondPackage(pkg)
                .status(TransactionStatus.PENDING)
                .provider(TransactionProvider.valueOf(request.getProvider()))
                .build();
        transaction = transactionRepository.save(transaction);
        String paymentUrl = "";
        if(request.getProvider().equals(TransactionProvider.VNPAY.name())) {
            paymentUrl = buildVNPayUrl(transaction, pkg);
        }

        transaction.setPaymentUrl(paymentUrl);
        transactionRepository.save(transaction);

        log.info("Created VNPay payment for userId={}, transactionId={}, amount={}",
                userId, transaction.getId(), pkg.getPrice());

        return CreatePaymentResponse
                .builder()
                .transactionId(transaction.getId())
                .provider(transaction.getProvider())
                .status(transaction.getStatus().name())
                .paymentUrl(paymentUrl)
                .build();

    }

    @Transactional
    public PaymentCallbackResponse proccessCallback(VnPayCallbackParams params) {
        // Verify chữ ký
        if(!verifySecureHash(params)) {
            log.warn("VNPay callback: invalid secure hash for txnRef={}", params.getVnp_TxnRef());

            return PaymentCallbackResponse.builder()
                    .message("Invalid signature")
                    .provider(TransactionProvider.VNPAY)
                    .transactionId(null)
                    .status(null)
                    .build();
        }

        // Tìm transaction
        Long transactionId = extractTransactionId(params.getVnp_TxnRef());
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // Idempotency: bỏ qua nếu đã xử lý
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("Transaction {} already processed with status={}", transactionId, transaction.getStatus());
            return buildCallbackResponse(transaction);
        }

        // Kiểm tra response code VNPay
        boolean isSuccess = "00".equals(params.getVnp_ResponseCode())
                && "00".equals(params.getVnp_TransactionStatus());

        if (isSuccess) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            // Cộng diamond cho user
            DiamondPackage pkg = transaction.getDiamondPackage();
            long totalDiamond = pkg.getDiamondAmount() + pkg.getBonusDiamond();
            diamondWalletService.topup(transaction.getUserId(), totalDiamond, transaction.getId());

            log.info("VNPay payment SUCCESS: userId={}, transactionId={}, diamonds={}",
                    transaction.getUserId(), transactionId, totalDiamond);

            return PaymentCallbackResponse.builder()
                    .message("Payment successful")
                    .provider(TransactionProvider.VNPAY)
                    .transactionId(transactionId)
                    .status(TransactionStatus.SUCCESS.name())
                    .build();
            
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            log.warn("VNPay payment FAILED: transactionId={}, responseCode={}",
                    transactionId, params.getVnp_ResponseCode());

            return PaymentCallbackResponse.builder()
                    .message(VNPayResponseCode.getMessage(params.getVnp_ResponseCode()))
                    .provider(TransactionProvider.VNPAY)
                    .transactionId(transactionId)
                    .status(TransactionStatus.FAILED.name())
                    .build();
        }
    }

    @Transactional
    public Map<String, String> processIPN(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();

        try {
            // Tách ra trước, KHÔNG remove khỏi map gốc ngay
            String vnpSecureHash = params.get("vnp_SecureHash");

            // Build map chỉ gồm các params cần hash (loại bỏ SecureHash fields)
            Map<String, String> hashParams = new HashMap<>(params);
            hashParams.remove("vnp_SecureHash");
            hashParams.remove("vnp_SecureHashType");

            // Dùng buildHashData (sorted, không encode) thay vì buildQueryString
            String hashData = VnPayUtils.buildHashData(hashParams);
            String calculatedHash = VnPayUtils.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

            if (!calculatedHash.equalsIgnoreCase(vnpSecureHash)) {
                log.warn("IPN invalid checksum. Expected={}, Got={}", calculatedHash, vnpSecureHash);
                result.put("RspCode", "97");
                result.put("Message", "Invalid Checksum");
                return result;
            }

            // Tìm transaction
            String txnRef = params.get("vnp_TxnRef");
            Long transactionId = extractTransactionId(txnRef);
            Transaction transaction = transactionRepository.findById(transactionId).orElse(null);

            if (transaction == null) {
                result.put("RspCode", "01");
                result.put("Message", "Order not Found");
                return result;
            }

            // Kiểm tra số tiền
            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100; // VNPay gửi * 100
            long expectedAmount = transaction.getDiamondPackage().getPrice().longValue();
            if (vnpAmount != expectedAmount) {
                result.put("RspCode", "04");
                result.put("Message", "Invalid Amount");
                return result;
            }

            // Idempotency
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                result.put("RspCode", "02");
                result.put("Message", "Order already confirmed");
                return result;
            }

            // Cập nhật trạng thái
            boolean isSuccess = "00".equals(params.get("vnp_ResponseCode"))
                    && "00".equals(params.get("vnp_TransactionStatus"));

            if (isSuccess) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(transaction);

                DiamondPackage pkg = transaction.getDiamondPackage();
                long totalDiamond = pkg.getDiamondAmount() + pkg.getBonusDiamond();
                diamondWalletService.topup(transaction.getUserId(), totalDiamond, transaction.getId());

                log.info("IPN SUCCESS: transactionId={}", transactionId);
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                log.warn("IPN FAILED: transactionId={}", transactionId);
            }

            result.put("RspCode", "00");
            result.put("Message", "Confirm Success");

        } catch (Exception e) {
            log.error("IPN processing error", e);
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }

        return result;
    }

    private String buildVNPayUrl(Transaction transaction, DiamondPackage pkg) {
        String txnRef = VnPayUtils.generateTxnRef(8) + "_" + transaction.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(15);

        // VNPay yêu cầu amount * 100 (không có dấu thập phân)
        long amount = pkg.getPrice().longValue() * 100;
        String clientIp = VnPayUtils.getClientIp();
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        log.info("TMN CODE " + vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Nap kim cuong goi " + pkg.getDiamondAmount()+ " kc");
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", VnPayUtils.formatVNPayDate(now));
        params.put("vnp_ExpireDate", VnPayUtils.formatVNPayDate(expireTime));

//        if (vnPayConfig.getIpnUrl() != null && !vnPayConfig.getIpnUrl().isBlank()) {
//            params.put("vnp_IpnUrl", vnPayConfig.getIpnUrl());
//        }

        // Tạo chuỗi hash (không encode)
        String hashData = VnPayUtils.buildHashData(params);
        String secureHash = VnPayUtils.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        log.info("Hash: "+vnPayConfig.getHashSecret());

        log.info("HASH DATA: {}", hashData);
        log.info("SECURE HASH: {}", secureHash);

        // Build URL (có encode)
        String queryString = VnPayUtils.buildQueryString(params);
        return vnPayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    private boolean verifySecureHash(VnPayCallbackParams params) {
        Map<String, String> map = new HashMap<>();
        map.put("vnp_TmnCode", params.getVnp_TmnCode());
        map.put("vnp_Amount", params.getVnp_Amount());
        map.put("vnp_BankCode", params.getVnp_BankCode());
        map.put("vnp_BankTranNo", params.getVnp_BankTranNo());
        map.put("vnp_CardType", params.getVnp_CardType());
        map.put("vnp_PayDate", params.getVnp_PayDate());
        map.put("vnp_OrderInfo", params.getVnp_OrderInfo());
        map.put("vnp_TransactionNo", params.getVnp_TransactionNo());
        map.put("vnp_ResponseCode", params.getVnp_ResponseCode());
        map.put("vnp_TransactionStatus", params.getVnp_TransactionStatus());
        map.put("vnp_TxnRef", params.getVnp_TxnRef());

        // Remove null values
        map.values().removeIf(v -> v == null || v.isEmpty());

        String hashData = VnPayUtils.buildHashData(map);
        String calculatedHash = VnPayUtils.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        return calculatedHash.equalsIgnoreCase(params.getVnp_SecureHash());
    }

    private Long extractTransactionId(String txnRef) {
        try {
            String[] parts = txnRef.split("_");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new RuntimeException("Cannot extract transactionId from txnRef: " + txnRef);
        }
    }

    private PaymentCallbackResponse buildCallbackResponse(Transaction transaction) {
        boolean success = transaction.getStatus() == TransactionStatus.SUCCESS;
        return PaymentCallbackResponse
                .builder()
                .message(success ? "Payment already processed successfully" : "Payment failed")
                .transactionId(transaction.getId())
                .status(transaction.getStatus().name())
                .provider(transaction.getProvider())
                .build();
    }
}

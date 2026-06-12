package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.WalletHistoryResponse;
import com.eefood.reactionservice.service.payment.DiamondWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/wallet")
@RequiredArgsConstructor
public class UserWalletController {
    private final DiamondWalletService diamondWalletService;

    @GetMapping("/{userId}/history")
    public ResponseData<Page<WalletHistoryResponse>> getWalletHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<WalletHistoryResponse> result = diamondWalletService.getWalletHistory(userId, type, sort, page, size);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }

    @GetMapping("/{userId}")
    public ResponseData<Long> getBalance(@PathVariable("userId") Long userId) {
        Long balance = diamondWalletService.getBalance(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", balance);
    }
}

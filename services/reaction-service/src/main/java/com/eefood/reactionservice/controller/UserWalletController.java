package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.payment.DiamondWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/wallet")
@RequiredArgsConstructor
public class UserWalletController {
    private final DiamondWalletService diamondWalletService;
    @GetMapping("/{userId}")
    public ResponseData<Long> getBalance(@PathVariable("userId") Long userId) {
        Long balance = diamondWalletService.getBalance(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", balance);
    }
}

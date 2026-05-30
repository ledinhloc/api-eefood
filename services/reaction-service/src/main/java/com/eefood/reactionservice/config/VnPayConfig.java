package com.eefood.reactionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class VnPayConfig {
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.api-url}")
    private String apiUrl;

    @Value("${vnpay.version}")
    private String version = "2.1.0";

    @Value("${vnpay.command}")
    private String command = "pay";

    @Value("${vnpay.order-type}")
    private String orderType = "other";

    @Value("${vnpay.locale}")
    private String locale = "vn";

    @Value("${vnpay.curr-code}")
    private String currCode = "VND";
}

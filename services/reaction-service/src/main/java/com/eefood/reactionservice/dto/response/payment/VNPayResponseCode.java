package com.eefood.reactionservice.dto.response.payment;
import lombok.Data;

import java.util.Map;

@Data
public final class VNPayResponseCode {

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("00", "Giao dịch thành công"),
            Map.entry("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)"),
            Map.entry("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking"),
            Map.entry("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần"),
            Map.entry("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán"),
            Map.entry("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa"),
            Map.entry("13", "Giao dịch không thành công do: Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)"),
            Map.entry("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch"),
            Map.entry("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch"),
            Map.entry("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày"),
            Map.entry("75", "Ngân hàng thanh toán đang bảo trì"),
            Map.entry("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định"),
            Map.entry("99", "Lỗi không xác định")
    );

    public static String getMessage(String code) {
        return MESSAGES.getOrDefault(code, "Giao dịch thất bại (mã lỗi: " + code + ")");
    }
}

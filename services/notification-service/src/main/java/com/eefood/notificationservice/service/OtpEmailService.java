package com.eefood.notificationservice.service;

import com.eefood.notificationservice.enums.OtpType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpEmailService {
  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  protected String emailSender;

  public void sendEmail(String recipientEmail, String otpCode, OtpType otpType) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(emailSender);
      helper.setTo(recipientEmail);
      helper.setSubject(
          otpType == OtpType.REGISTER ? "Confirm Your Registration" : "OTP for Password Reset");

      String emailContent =
              "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width:600px; margin:auto; padding:0; background-color:#f8f9fa;\">"
              + "<div style=\"background: linear-gradient(135deg, #FF6B35 0%, #FF8C42 100%); padding:30px 20px; text-align:center; border-radius:12px 12px 0 0;\">"
              + "<div style=\"background-color:white; width:80px; height:80px; margin:0 auto 15px; border-radius:50%; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 12px rgba(0,0,0,0.15);\">"
              + "<span style=\"font-size:36px;\">🍜</span>"
              + "</div>"
              + "<h1 style=\"color:#ffffff; margin:0; font-size:28px; font-weight:600; text-shadow:0 2px 4px rgba(0,0,0,0.1);\">eeFood</h1>"
              + "<p style=\"color:#fff; margin:8px 0 0 0; font-size:14px; opacity:0.95;\">Khám phá ẩm thực mỗi ngày</p>"
              + "</div>"
              + "<div style=\"background-color:#ffffff; padding:40px 30px; border-radius:0 0 12px 12px; box-shadow:0 2px 8px rgba(0,0,0,0.08);\">"
              + "<h2 style=\"color:#2c3e50; font-size:22px; margin:0 0 20px 0; font-weight:600;\">Xin chào! 👋</h2>"
              + "<p style=\"color:#5a6c7d; line-height:1.6; font-size:15px; margin:0 0 25px 0;\">"
              + "Để "
              + (otpType == OtpType.REGISTER ? "<strong>hoàn tất đăng ký tài khoản</strong>" : "<strong>đặt lại mật khẩu</strong>")
              + ", vui lòng sử dụng mã OTP bên dưới. Mã này có hiệu lực trong <strong style=\"color:#FF6B35;\">15 phút</strong>."
              + "</p>"
              + "<div style=\"background: linear-gradient(135deg, #E3F2FD 0%, #E8F5E9 100%); padding:30px; border-radius:12px; text-align:center; margin:30px 0; border:2px dashed #FF6B35;\">"
              + "<p style=\"color:#5a6c7d; font-size:13px; margin:0 0 10px 0; text-transform:uppercase; letter-spacing:1px; font-weight:600;\">Mã OTP của bạn</p>"
              + "<div style=\"font-size:36px; font-weight:bold; color:#FF6B35; letter-spacing:8px; font-family:'Courier New', monospace;\">"
              + otpCode
              + "</div>"
              + "</div>"
              + "<div style=\"background-color:#FFF3E0; border-left:4px solid #FF8C42; padding:15px 20px; border-radius:6px; margin:25px 0;\">"
              + "<p style=\"color:#E65100; font-size:13px; margin:0; line-height:1.5;\">"
              + "<strong>⚠️ Lưu ý:</strong> Không chia sẻ mã OTP này với bất kỳ ai. Đội ngũ eeFood sẽ không bao giờ yêu cầu mã OTP qua điện thoại hoặc email."
              + "</p>"
              + "</div>"
              + "<p style=\"color:#5a6c7d; line-height:1.6; font-size:15px; margin:30px 0 0 0;\">"
              + "Chúc bạn có trải nghiệm tuyệt vời cùng eeFood! 🎉"
              + "</p>"
              + "<p style=\"color:#5a6c7d; font-size:15px; margin:10px 0 0 0;\">"
              + "Trân trọng,<br/><strong style=\"color:#FF6B35;\">Đội ngũ eeFood</strong>"
              + "</p>"
              + "</div>"
              + "<div style=\"padding:20px; text-align:center;\">"
              + "<div style=\"margin:20px 0;\">"
              + "<a href=\"#\" style=\"display:inline-block; margin:0 10px; color:#5a6c7d; text-decoration:none; font-size:24px;\">📱</a>"
              + "<a href=\"#\" style=\"display:inline-block; margin:0 10px; color:#5a6c7d; text-decoration:none; font-size:24px;\">🌐</a>"
              + "<a href=\"#\" style=\"display:inline-block; margin:0 10px; color:#5a6c7d; text-decoration:none; font-size:24px;\">📧</a>"
              + "</div>"
              + "<p style=\"font-size:12px; color:#95a5a6; margin:10px 0; line-height:1.5;\">"
              + "© 2024 eeFood Inc. All rights reserved.<br/>"
              + "1600 Amphitheatre Parkway, California"
              + "</p>"
              + "<p style=\"font-size:11px; color:#bdc3c7; margin:15px 0 0 0;\">"
              + "Email này được gửi tự động, vui lòng không trả lời."
              + "</p>"
              + "</div>"
              + "</div>";

      helper.setText(emailContent, true); // true = HTML

      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
}

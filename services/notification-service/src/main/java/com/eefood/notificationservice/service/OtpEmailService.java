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
          "<div style=\"font-family: Arial, sans-serif; max-width:600px; margin:auto; padding:20px; border:1px solid #eee; border-radius:8px;\">"
              + "<h2 style=\"color:#00466a; text-align:center;\">Echo English</h2>"
              + "<p>Hi,</p>"
              + "<p>Use the following OTP to "
              + (otpType == OtpType.REGISTER ? "complete your registration" : "reset your password")
              + ". OTP is valid for 15 minutes.</p>"
              + "<div style=\"text-align:center; margin:20px 0;\">"
              + "<span style=\"font-size:32px; font-weight:bold; color:#fff; background-color:#00466a; padding:10px 20px; border-radius:6px;\">"
              + otpCode
              + "</span>"
              + "</div>"
              + "<p>Regards,<br/>EE FOOD Team</p>"
              + "<hr style=\"border:none; border-top:1px solid #eee; margin-top:20px;\"/>"
              + "<p style=\"font-size:12px; color:#aaa; text-align:center;\">Echo Inc, 1600 Amphitheatre Parkway, California</p>"
              + "</div>";

      helper.setText(emailContent, true); // true = HTML

      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
}

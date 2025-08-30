package com.eefood.iamservice.service;
import com.eefood.iamservice.dto.request.OtpCreateRequest;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.OtpType;
import com.eefood.iamservice.model.Otp;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.producer.EmailProducer;
import com.eefood.iamservice.repository.OtpRepository;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailProducer emailProducer;

    // Hàm tạo mã OTP
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Hàm gửi OTP
    public OtpCreateRequest sendOtp(String email, OtpType otpType)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> ExceptionUtil.notFound(ErrorMessage.USER_NOT_FOUND));

        String otpCode = generateOtp();

        Otp otp = Otp.builder()
                .otpNum(otpCode)
                .isDeleted(false)
                .otpExpired(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        OtpCreateRequest otpRequest = OtpCreateRequest.builder()
                .otpCode(otpCode)
                .otpType(otpType)
                .email(email)
                .build();

        otpRepository.save(otp);

        emailProducer.sendEmailProducerEvent(otpRequest);

        return otpRequest;
    }
}

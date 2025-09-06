package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.*;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.OtpType;
import com.eefood.iamservice.enums.SuccessMessage;
import com.eefood.iamservice.mapper.UserMapper;
import com.eefood.iamservice.service.AuthService;
import com.eefood.iamservice.service.KeycloakAdminService;
import com.eefood.iamservice.service.OtpService;
import com.eefood.iamservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final OtpService otpService;
    private final UserService userService;
    private final KeycloakAdminService keycloakService;

    //login
    @PostMapping("/login")
    public ResponseData<?> login(@RequestBody @Valid LoginRequest request) {
      return new ResponseData<>(HttpStatus.OK.value(), "Success", userService.login(request.getEmail(), request.getPassword()));
    }

    //refresh token
    @PostMapping("/refresh")
    public  ResponseData<?> refreshToken(@RequestBody RefreshTokenRequest request){
        return new ResponseData<>(HttpStatus.OK.value(), "Success", keycloakService.refreshToken(request.getRefreshToken()));
    }

    //logout
    @PostMapping("/logout")
    public  ResponseData<?> logout(@RequestBody RefreshTokenRequest request){
        keycloakService.logout(request.getRefreshToken());
        return new ResponseData<>(HttpStatus.OK.value(), "Success");
    }

    @PostMapping("/register")
    public ResponseData<UserResponse> register(@Valid @RequestBody UserSignUpRequest request) {
        UserResponse userResponse = authService.registerUser(request);
        return (userResponse == null)
                ? new ResponseData<>(HttpStatus.BAD_REQUEST.value(), ErrorMessage.USER_NOT_FOUND.getMessage())
                : new ResponseData<>(HttpStatus.CREATED.value(), SuccessMessage.CREATE_USER_SUCCESS.getMessage(), userResponse);
    }

    @PostMapping("/verify-otp")
    public ResponseData<Void> verifyOtp(@Valid  @RequestBody OtpCreateRequest request) {
        boolean verified = authService.verifyOtp(request.getEmail(),request.getOtpCode());
        return verified
                ? new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.OTP_VERIFIED_SUCCESS.getMessage())
                : new ResponseData<>(HttpStatus.BAD_REQUEST.value(), ErrorMessage.OTP_INVALID_OR_EXPIRED.getMessage());
    }

    @PostMapping("/forgot-password/request")
    public ResponseData<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        otpService.sendOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.OTP_SENT.getMessage());
    }

    @PostMapping("/forgot-password/reset")
    public ResponseData<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request){
        boolean verified = authService.verifyOtp(request.getEmail(), request.getOtp());
        if(!verified){
            return new ResponseData<>(HttpStatus.BAD_REQUEST.value(), ErrorMessage.OTP_INVALID_OR_EXPIRED.getMessage());
        }
        authService.resetPassword(request.getEmail(),request.getOtp());
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.PASSWORD_RESET_SUCCESS.getMessage());
    }

}

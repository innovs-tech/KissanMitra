package com.kissanmitra.controller;

import com.kissanmitra.enums.Response;
import com.kissanmitra.request.OtpVerifyRequest;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public BaseClientResponse<String> sendOtp(@RequestParam String phoneNumber) {
        String result = authService.sendOtp(phoneNumber);
        return Response.SUCCESS.buildSuccess(generateRequestId(), result);
    }

    @PostMapping("/verify-otp")
    public BaseClientResponse<AuthResponse> verifyOtp(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        AuthResponse result = authService.verifyOtp(otpVerifyRequest.getPhoneNumber(), otpVerifyRequest.getOtp());
        return Response.SUCCESS.buildSuccess(generateRequestId(), result);
    }
}


package com.kissanmitra.service.impl;

import com.kissanmitra.exception.InvalidOtpException;
import com.kissanmitra.exception.OtpExpiredException;
import com.kissanmitra.response.AuthResponse;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.security.JwtUtil;
import com.kissanmitra.service.AuthService;
import com.kissanmitra.service.UserService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.fromNumber}")
    private String fromNumber;

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final UserService userService;


    @Override
    public String sendOtp(String phoneNumber) {
        Twilio.init(accountSid, authToken);

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Save OTP to Redis for 5 minutes
        redisTemplate.opsForValue().set("OTP:" + phoneNumber, otp, Duration.ofMinutes(5));
        String rawMobile = phoneNumber.trim();
        String mobile = rawMobile.startsWith("+91") ? rawMobile : "+91" + rawMobile;



        Message.creator(
                new com.twilio.type.PhoneNumber(mobile),
                new com.twilio.type.PhoneNumber(fromNumber),
                "Your OTP is: " + otp + " (valid for 5 min)"
        ).create();

        return "OTP sent successfully.";
    }

    @Override
    public AuthResponse verifyOtp(String phoneNumber, String otp) {
        String key = "OTP:" + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new OtpExpiredException("OTP expired or not found");
        }

        if (!storedOtp.equals(otp)) {
            throw new InvalidOtpException("Invalid OTP");
        }

        UserResponse userResponse = userService.ensureUserExistsOrSaveUser(phoneNumber);

        redisTemplate.delete(key);

        String token = jwtUtil.generateToken(phoneNumber);

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }


}


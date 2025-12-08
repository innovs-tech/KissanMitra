package com.kissanmitra.service;

import com.kissanmitra.response.AuthResponse;

public interface AuthService {
    String sendOtp(String phoneNumber);
    AuthResponse verifyOtp(String phoneNumber, String otp);
}

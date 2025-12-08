package com.kissanmitra.exception;

import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OtpExpiredException.class)
    public BaseClientResponse<?> handleExpiredOtp(OtpExpiredException ex) {
        return Response.UNAUTHORIZED.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),   // correlation id
                "OTP expired",                   // frontend msg
                ex.getMessage()                  // backend msg
        );
    }

    @ExceptionHandler(InvalidOtpException.class)
    public BaseClientResponse<?> handleInvalidOtp(InvalidOtpException ex) {
        return Response.BAD_REQUEST.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),
                "Invalid OTP",
                ex.getMessage()
        );
    }
}

package com.kissanmitra.exception;

import com.kissanmitra.enums.Response;
import com.kissanmitra.response.BaseClientResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
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

    /**
     * Handles validation errors (Jakarta Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseClientResponse<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return Response.BAD_REQUEST.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),
                "Validation failed",
                errors.toString()
        );
    }

    /**
     * Handles IllegalArgumentException (invalid arguments).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseClientResponse<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Response.BAD_REQUEST.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),
                "Invalid request",
                ex.getMessage()
        );
    }

    /**
     * Handles RuntimeException (business logic errors, not found, etc.).
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseClientResponse<?> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Check if it's a "not found" error
        if (message != null && message.toLowerCase().contains("not found")) {
            return Response.NOT_FOUND.buildErrorWithCustomMessage(
                    UUID.randomUUID().toString(),
                    "Resource not found",
                    message
            );
        }

        // Check if it's an "already exists" or duplicate error
        if (message != null && (message.toLowerCase().contains("already exists") ||
                                message.toLowerCase().contains("duplicate"))) {
            return Response.BAD_REQUEST.buildErrorWithCustomMessage(
                    UUID.randomUUID().toString(),
                    "Resource already exists",
                    message
            );
        }

        // Default to bad request for other runtime exceptions
        return Response.BAD_REQUEST.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),
                "Operation failed",
                message != null ? message : "An error occurred"
        );
    }

    /**
     * Handles all other exceptions (database errors, unexpected errors).
     */
    @ExceptionHandler(Exception.class)
    public BaseClientResponse<?> handleGenericException(Exception ex) {
        // Log the full exception for debugging
        ex.printStackTrace();

        return Response.SERVER_ERROR.buildErrorWithCustomMessage(
                UUID.randomUUID().toString(),
                "Internal server error",
                "An unexpected error occurred. Please try again later."
        );
    }
}

package com.kissanmitra.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Standardized response wrapper for all API responses.
 * Provides consistent structure across all endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseClientResponse<T> implements Serializable {

    private boolean success;
    private String message;
    private T data;
    private String correlationId;
    private Instant timestamp;
    private String errorCode;
    private String errorDetails;

    /**
     * Creates a successful response
     */
    public static <T> BaseClientResponse<T> success(T data, String message) {
        return BaseClientResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful response with correlation ID
     */
    public static <T> BaseClientResponse<T> success(T data, String message, String correlationId) {
        return BaseClientResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error response
     */
    public static <T> BaseClientResponse<T> error(String message, String errorCode) {
        return BaseClientResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error response with details
     */
    public static <T> BaseClientResponse<T> error(String message, String errorCode, String errorDetails) {
        return BaseClientResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timestamp(Instant.now())
                .build();
    }
}

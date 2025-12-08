package com.kissanmitra.enums;

import com.kissanmitra.response.BaseClientResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public enum Response {

    SUCCESS("200", "Operation successful", "The request was processed successfully."),
    CREATED("201", "Resource created", "A new resource has been successfully created."),
    UPDATED("200", "Resource updated", "The resource was successfully updated."),
    BAD_REQUEST("400", "Invalid request", "The request was malformed or missing required parameters."),
    UNAUTHORIZED("401", "Unauthorized", "Authentication is required to access this resource."),
    FORBIDDEN("403", "Access denied", "You do not have permission to perform this action."),
    NOT_FOUND("404", "Resource not found", "The requested resource could not be found."),
    SERVER_ERROR("500", "Server error", "An unexpected error occurred on the server.");

    private final String code;
    private final String frontendMessage;
    private final String backendMessage;

    public <T> BaseClientResponse<T> buildSuccess(String requestId, T data) {
        return BaseClientResponse.<T>builder()
                .success(true)
                .message(this.frontendMessage)
                .data(data)
                .correlationId(requestId)
                .timestamp(Instant.now())
                .build();
    }

    public <T> BaseClientResponse<T> buildSuccessWithCustomMessage(String requestId, T data, String frontendMessage, String backendMessage) {
        return BaseClientResponse.<T>builder()
                .success(true)
                .message(frontendMessage)
                .data(data)
                .correlationId(requestId)
                .timestamp(Instant.now())
                .build();
    }

    public <T> BaseClientResponse<T> buildError(String requestId) {
        return BaseClientResponse.<T>builder()
                .success(false)
                .message(this.frontendMessage)
                .errorCode(this.code)
                .correlationId(requestId)
                .timestamp(Instant.now())
                .build();
    }

    public <T> BaseClientResponse<T> buildErrorWithCustomMessage(String requestId, String frontendMessage, String backendMessage) {
        return BaseClientResponse.<T>builder()
                .success(false)
                .message(frontendMessage)
                .errorCode(this.code)
                .errorDetails(backendMessage)
                .correlationId(requestId)
                .timestamp(Instant.now())
                .build();
    }

}



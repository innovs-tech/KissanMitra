package com.kissanmitra.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for push token registration.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushTokenRequest {

    /**
     * Platform (ANDROID, IOS).
     */
    @NotNull(message = "Platform is required")
    private String platform;

    /**
     * FCM token.
     */
    @NotNull(message = "Token is required")
    private String token;
}


package com.kissanmitra.controller;

import com.kissanmitra.config.UserContext;
import com.kissanmitra.enums.Response;
import com.kissanmitra.request.PushTokenRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

/**
 * Controller for device-related operations (push token registration).
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class PushTokenController {

    private final NotificationService notificationService;
    private final UserContext userContext;

    /**
     * Registers a push token for the current user.
     *
     * @param request push token request
     * @return success message
     */
    @PostMapping("/push-token")
    public BaseClientResponse<String> registerPushToken(@Valid @RequestBody final PushTokenRequest request) {
        final String userId = userContext.getCurrentUserId();
        notificationService.registerPushToken(userId, request.getPlatform(), request.getToken());
        return Response.SUCCESS.buildSuccess(generateRequestId(), "Push token registered successfully");
    }
}


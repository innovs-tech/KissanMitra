package com.kissanmitra.entity;

import com.kissanmitra.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Push token entity for FCM token storage.
 *
 * <p>Business Context:
 * - Mobile apps register FCM tokens
 * - Tokens used for push notifications
 * - Tokens can be refreshed
 */
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(collection = "push_tokens")
public class PushToken extends BaseEntity {

    /**
     * User ID who owns the token.
     */
    @Indexed
    private String userId;

    /**
     * Platform (ANDROID, IOS).
     */
    private String platform;

    /**
     * FCM token value.
     */
    @Indexed(unique = true)
    private String token;

    /**
     * Whether token is active.
     */
    private Boolean active;

    /**
     * Timestamp of last seen.
     */
    private Instant lastSeenAt;
}


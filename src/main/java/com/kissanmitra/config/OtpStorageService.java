package com.kissanmitra.config;

import java.time.Duration;

/**
 * Interface for OTP storage operations.
 *
 * <p>Abstracts OTP storage to allow different implementations:
 * <ul>
 *   <li>Redis-based (production)</li>
 *   <li>In-memory (local development)</li>
 * </ul>
 */
public interface OtpStorageService {

    /**
     * Stores an OTP with expiration.
     *
     * @param key OTP key (e.g., "OTP:+919876543210")
     * @param otp OTP value
     * @param expiration expiration duration
     */
    void set(String key, String otp, Duration expiration);

    /**
     * Retrieves an OTP by key.
     *
     * @param key OTP key
     * @return OTP value, or null if not found/expired
     */
    String get(String key);

    /**
     * Deletes an OTP by key.
     *
     * @param key OTP key
     */
    void delete(String key);
}


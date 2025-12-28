package com.kissanmitra.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis-based OTP storage implementation.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOtpStorageService implements OtpStorageService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void set(final String key, final String otp, final Duration expiration) {
        try {
            redisTemplate.opsForValue().set(key, otp, expiration);
            log.debug("Stored OTP in Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to store OTP in Redis: {}", e.getMessage());
            throw new RuntimeException("Failed to store OTP", e);
        }
    }

    @Override
    public String get(final String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to retrieve OTP from Redis: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(final String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted OTP from Redis: {}", key);
        } catch (Exception e) {
            log.warn("Failed to delete OTP from Redis: {}", e.getMessage());
        }
    }
}


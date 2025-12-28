package com.kissanmitra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configuration for OTP storage.
 *
 * <p>Provides two implementations:
 * <ul>
 *   <li>Redis-based storage (when Redis is available)</li>
 *   <li>In-memory storage (fallback for local development)</li>
 * </ul>
 *
 * <p>Set `spring.data.redis.enabled=false` to use in-memory storage.
 */
@Configuration
public class OtpStorageConfig {

    /**
     * Redis-based OTP storage (primary, when Redis is available).
     */
    @Bean
    @Primary
    @ConditionalOnBean(StringRedisTemplate.class)
    public OtpStorageService redisOtpStorage(final StringRedisTemplate redisTemplate) {
        return new RedisOtpStorageService(redisTemplate);
    }

    /**
     * In-memory OTP storage (fallback when Redis is not available).
     */
    @Bean
    @ConditionalOnMissingBean(OtpStorageService.class)
    public OtpStorageService inMemoryOtpStorage() {
        return new InMemoryOtpStorageService();
    }
}


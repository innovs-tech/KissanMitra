package com.kissanmitra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for master data.
 *
 * <p>Business Context:
 * - Caches DeviceType and Manufacturer lookups
 * - Simple in-memory cache (can be upgraded to Redis/Caffeine later)
 * - Cache eviction on updates
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Creates cache manager for master data.
     *
     * @return cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        // BUSINESS DECISION: Use simple in-memory cache
        // Future: Can upgrade to Caffeine or Redis for distributed caching
        return new ConcurrentMapCacheManager("deviceTypes", "manufacturers");
    }
}


package com.kissanmitra.config;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory OTP storage implementation for local development.
 *
 * <p>Stores OTPs in memory with automatic expiration.
 * This is a fallback when Redis is not available.
 *
 * <p>Note: OTPs are lost on application restart.
 */
@Slf4j
public class InMemoryOtpStorageService implements OtpStorageService {

    private static final ScheduledExecutorService cleanupExecutor = 
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "otp-cleanup");
                t.setDaemon(true);
                return t;
            });

    private final Map<String, OtpEntry> storage = new ConcurrentHashMap<>();

    public InMemoryOtpStorageService() {
        // Start cleanup task to remove expired entries
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
        log.info("Using in-memory OTP storage (Redis not available)");
    }

    @Override
    public void set(final String key, final String otp, final Duration expiration) {
        final Instant expiresAt = Instant.now().plus(expiration);
        storage.put(key, new OtpEntry(otp, expiresAt));
        log.debug("Stored OTP in memory: {} (expires at: {})", key, expiresAt);
    }

    @Override
    public String get(final String key) {
        final OtpEntry entry = storage.get(key);
        if (entry == null) {
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            storage.remove(key);
            log.debug("OTP expired: {}", key);
            return null;
        }

        return entry.otp;
    }

    @Override
    public void delete(final String key) {
        storage.remove(key);
        log.debug("Deleted OTP from memory: {}", key);
    }

    private void cleanupExpiredEntries() {
        final Instant now = Instant.now();
        storage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    private record OtpEntry(String otp, Instant expiresAt) {
    }
}


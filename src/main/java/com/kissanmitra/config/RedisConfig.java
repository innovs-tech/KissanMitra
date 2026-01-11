package com.kissanmitra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis configuration.
 *
 * <p>Only creates Redis beans when Redis is enabled.
 * Set `spring.data.redis.enabled=false` to disable Redis and use in-memory storage.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    public RedisConfig() {
        log.info("RedisConfig class instantiated");
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("=== RedisConfig.redisConnectionFactory() called ===");
        log.info("Environment variable SPRING_DATA_REDIS_HOST: {}", System.getenv("SPRING_DATA_REDIS_HOST"));
        log.info("Property spring.data.redis.host value: {}", redisHost);

        final String host = redisHost != null ? redisHost : "localhost";
        log.info("Configuring Redis connection - Host: {}, Port: {}", host, redisPort);

        if (host.trim().isEmpty() || host.equals("localhost")) {
            log.error("Redis host is not properly configured! Current value: '{}'. Expected ElastiCache endpoint from SPRING_DATA_REDIS_HOST environment variable.", host);
            log.error("Please verify that SPRING_DATA_REDIS_HOST is set in the ECS task definition secrets.");
        }

        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(redisPort);

        final LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        log.info("Redis connection factory created for {}:{}", host, redisPort);
        return factory;
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }
}

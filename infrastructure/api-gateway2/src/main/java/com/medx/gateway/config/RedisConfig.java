package com.medx.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /*
     * ReactiveRedisTemplate — used by Spring Cloud Gateway's RequestRateLimiter filter
     * internally.
     * We use String serializer for both key and value because rate limiter stores simple
     * string keys (IP or user ID) and counters.
     * DB index 2 is configured in application.yml under spring.data.redis.database — no
     * need to set it here again.
     * This bean just provides the correct reactive template wired to that connection factory.
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
        ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> context =
            RedisSerializationContext.<String, String>newSerializationContext(serializer)
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

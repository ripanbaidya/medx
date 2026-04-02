package com.medx.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.medx.gateway.constant.GatewayConstant.*;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Configuration class for Spring Cloud Gateway routing.
 * It defines routes for various microservices in the application, including
 * rate limiting, circuit breakers, retries, and fallbacks.
 * Routes are configured with appropriate security and resilience patterns.
 */
@Configuration
public class GatewayConfig {

    /*
     * Two KeyResolver beans injected by name.
     * ipKeyResolver: for public routes (no user context)
     * userKeyResolver: for authenticated routes (per user bucket)
     */
    private final KeyResolver ipKeyResolver;
    private final KeyResolver userKeyResolver;

    public GatewayConfig(
        @Qualifier(RATE_LIMITER_KEY_IP) KeyResolver ipKeyResolver,
        @Qualifier(RATE_LIMITER_KEY_USER) KeyResolver userKeyResolver
    ) {
        this.ipKeyResolver = ipKeyResolver;
        this.userKeyResolver = userKeyResolver;
    }

    // Rate Limiter Factories
    // replenishRate  — tokens added per second (steady state RPS)
    // burstCapacity  — max tokens in bucket (burst allowance)
    // requestedTokens — tokens consumed per request (always 1)
    //
    // Example: replenish=20, burst=40
    //   → Normally 20 req/sec allowed
    //   → Burst of up to 40 req/sec for a short period
    //   → After burst, back to 20 req/sec

    /**
     * Creates a rate limiter for public routes.
     * Stricter limits to handle anonymous traffic and prevent abuse.
     *
     * @return RedisRateLimiter configured for public routes
     */
    private RedisRateLimiter publicRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Creates a rate limiter for authenticated user routes.
     * More lenient limits for trusted users.
     *
     * @return RedisRateLimiter configured for user routes
     */
    private RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    /**
     * Creates a rate limiter for payment routes.
     * Strictest limits to prevent payment spam and fraud.
     *
     * @return RedisRateLimiter configured for payment routes
     */
    private RedisRateLimiter paymentRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Creates a rate limiter for admin routes.
     * Generous limits for low-volume, trusted admin operations.
     *
     * @return RedisRateLimiter configured for admin routes
     */
    private RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    /**
     * Defines the route locator for all microservices.
     * Each route includes appropriate filters for rate limiting, circuit breaking,
     * retries, and request headers.
     *
     * @param builder RouteLocatorBuilder for constructing routes
     * @return RouteLocator with all configured routes
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // USER SERVICE
            // Handles: patient registration, profile, photo upload
            .route(ROUTE_USER_SERVICE, r -> r
                .path(USER_PATH)
                .filters(f -> f
                    // Tag request as coming from gateway
                    .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                    // Rate limit per user ID (authenticated route)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(userRateLimiter())
                        .setKeyResolver(userKeyResolver)
                    )
                    // Circuit breaker — fallback if user-service is down
                    .circuitBreaker(config -> config
                        .setName(CB_USER)
                        .setFallbackUri(FALLBACK_USER)
                    )
                    // Retry on transient failures (not 4xx)
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_USER)
            )

            // DOCTOR SERVICE
            // Handles: public browsing + authenticated doctor actions
            // Mixed route — some paths public, some protected
            // Security handled by SecurityConfig, not here
            .route(ROUTE_DOCTOR_SERVICE, r -> r
                .path(DOCTOR_PATH)
                .filters(f -> f
                    .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                    // IP-based for public doctor browsing paths
                    .requestRateLimiter(config -> config
                        .setRateLimiter(publicRateLimiter())
                        .setKeyResolver(ipKeyResolver)
                    )
                    .circuitBreaker(config -> config
                        .setName(CB_DOCTOR)
                        .setFallbackUri(FALLBACK_DOCTOR)
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_DOCTOR)
            )

            // APPOINTMENT SERVICE
            // Critical service — lower retry, stricter CB
            .route(ROUTE_APPOINTMENT_SERVICE, r -> r
                .path(APPOINTMENT_PATH)
                .filters(f -> f
                    .addRequestHeader(
                        GATEWAY_SOURCE_HEADER,
                        GATEWAY_SOURCE_VALUE
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(userRateLimiter())
                        .setKeyResolver(userKeyResolver)
                    )
                    .circuitBreaker(config -> config
                        .setName(CB_APPOINTMENT)
                        .setFallbackUri(FALLBACK_APPOINTMENT)
                    )
                    // Only 2 retries — avoid double booking risk
                    .retry(config -> config
                        .setRetries(2)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_APPOINTMENT)
            )

            // PAYMENT SERVICE
            // Most critical — no retry (idempotency key handles it)
            // Strictest rate limiter
            .route(ROUTE_PAYMENT_SERVICE, r -> r
                .path(PAYMENT_PATH)
                .filters(f -> f
                        .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                        .requestRateLimiter(config -> config
                            .setRateLimiter(paymentRateLimiter())
                            .setKeyResolver(userKeyResolver)
                        )
                        .circuitBreaker(config -> config
                            .setName(CB_PAYMENT)
                            .setFallbackUri(FALLBACK_PAYMENT)
                        )
                    // No retry on payment — idempotency key in payment-service
                    // handles retries safely from client side
                )
                .uri(SVC_PAYMENT)
            )

            // NOTIFICATION SERVICE — REST
            .route(ROUTE_NOTIFICATION_SERVICE, r -> r
                .path(NOTIFICATION_PATH)
                .filters(f -> f
                    .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(userRateLimiter())
                        .setKeyResolver(userKeyResolver)
                    )
                    .circuitBreaker(config -> config
                        .setName(CB_NOTIFICATION)
                        .setFallbackUri(FALLBACK_NOTIFICATION)
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_NOTIFICATION)
            )

            // NOTIFICATION SERVICE — WebSocket
            // Separate route for WS upgrade requests.
            // WebSocket uses HTTP Upgrade — must be routed separately.
            // No rate limiter on WS — connection is persistent,
            // not per-request. Auth handled by the WS handshake.
            .route(ROUTE_NOTIFICATION_SERVICE + "-ws", r -> r
                .path(NOTIFICATION_WS_PATH)
                .uri(SVC_NOTIFICATION)
            )

            // FEEDBACK SERVICE
            // Mixed — some public (read), some patient-only (write)
            .route(ROUTE_FEEDBACK_SERVICE, r -> r
                .path(FEEDBACK_PATH)
                .filters(f -> f
                    .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(publicRateLimiter())
                        .setKeyResolver(ipKeyResolver)
                    )
                    .circuitBreaker(config -> config
                        .setName(CB_FEEDBACK)
                        .setFallbackUri(FALLBACK_FEEDBACK)
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_FEEDBACK)
            )

            // ADMIN SERVICE
            // Admin only — generous rate limit, separate CB
            .route(ROUTE_ADMIN_SERVICE, r -> r
                .path(ADMIN_PATH)
                .filters(f -> f
                    .addRequestHeader(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(adminRateLimiter())
                        .setKeyResolver(userKeyResolver)
                    )
                    .circuitBreaker(config -> config
                        .setName(CB_ADMIN)
                        .setFallbackUri(FALLBACK_ADMIN)
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                    )
                )
                .uri(SVC_ADMIN)
            )

            .build();
    }
}
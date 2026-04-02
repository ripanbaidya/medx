package com.medx.gateway.config;

import com.medx.gateway.constants.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Configuration
public class GatewayConfig {

    private final KeyResolver ipKeyResolver;
    private final KeyResolver userKeyResolver;

    @Autowired
    public GatewayConfig(
            @Qualifier(RateLimiterConstants.KEY_RESOLVER_IP) KeyResolver ipKeyResolver,
            @Qualifier(RateLimiterConstants.KEY_RESOLVER_USER) KeyResolver userKeyResolver) {
        this.ipKeyResolver = ipKeyResolver;
        this.userKeyResolver = userKeyResolver;
    }

    /*
     * Rate limiter factories
     * replenishRater - tokens added per second (steady RPS)
     * burstCapacity - max tokens (burst allowance)
     * requestedTokens - tokens consumed per request (usually 1)
     */

    // Public routes - (anonymous traffic handling, abuse risk)
    private RedisRateLimiter publicRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    // Authenticated user routes - more lenient (truster users)
    private RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    // Payments routes - strictest (prevent payment spam/ fraud)
    private RedisRateLimiter paymentRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    // Admin routes - generous (low volume, trusted operators)
    private RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                /*
                 * user-service
                 */
                .route(RouteConstants.ROUTE_USER_SERVICE, r -> r
                        .path(PathConstants.USER_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(userRateLimiter())
                                        .setKeyResolver(userKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_USER)
                                        .setFallbackUri(RouteConstants.FALLBACK_USER)
                                )
                                .retry(c -> c
                                        .setRetries(3)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_USER)
                )

                /*
                 * doctor-service
                 * This route is mixed of public and protected - security config handled the authentication per path
                 * IP-based
                 */
                .route(RouteConstants.ROUTE_DOCTOR_SERVICE, r -> r
                        .path(PathConstants.DOCTOR_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(publicRateLimiter())
                                        .setKeyResolver(ipKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_DOCTOR)
                                        .setFallbackUri(RouteConstants.FALLBACK_DOCTOR)
                                )
                                .retry(c -> c
                                        .setRetries(3)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_DOCTOR)
                )

                /*
                 * appointment-service
                 * this is critical - only 2 retries to avoid double-booking risk.
                 */
                .route(RouteConstants.ROUTE_APPOINTMENT_SERVICE, r -> r
                        .path(PathConstants.APPOINTMENT_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(userRateLimiter())
                                        .setKeyResolver(userKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_APPOINTMENT)
                                        .setFallbackUri(RouteConstants.FALLBACK_APPOINTMENT)
                                )
                                .retry(c -> c
                                        .setRetries(2)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_APPOINTMENT)
                )

                /*
                 * payment-service
                 * This is most critical — zero gateway-level retries.
                 * Client retries using idempotency key; payment-service handles dedup.
                 */
                .route(RouteConstants.ROUTE_PAYMENT_SERVICE, r -> r
                        .path(PathConstants.PAYMENT_PATH)
                        .filters(f -> f
                                        .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(paymentRateLimiter())
                                                .setKeyResolver(userKeyResolver)
                                        )
                                        .circuitBreaker(c -> c
                                                .setName(CircuitBreakerConstants.CB_PAYMENT)
                                                .setFallbackUri(RouteConstants.FALLBACK_PAYMENT)
                                        )
                                // No retry — idempotency key on client side handles it safely
                        )
                        .uri(RouteConstants.SVC_PAYMENT)
                )

                /*
                 * notification-service
                 */
                .route(RouteConstants.ROUTE_NOTIFICATION_SERVICE, r -> r
                        .path(PathConstants.NOTIFICATION_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(userRateLimiter())
                                        .setKeyResolver(userKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_NOTIFICATION)
                                        .setFallbackUri(RouteConstants.FALLBACK_NOTIFICATION)
                                )
                                .retry(c -> c
                                        .setRetries(3)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_NOTIFICATION)
                )

                /*
                 * notification-service: websocket endpoint
                 * Separate route — HTTP Upgrade must be matched independently.
                 * No rate limiter (connection is persistent, not per-request).
                 * No circuit breaker on WS path (CB doesn't apply to upgrade handshake).
                 */
                .route(RouteConstants.ROUTE_NOTIFICATION_SERVICE + "-ws", r -> r
                        .path(PathConstants.NOTIFICATION_WS_PATH)
                        .uri(RouteConstants.SVC_NOTIFICATION)
                )

                /*
                 * feedback-service
                 * Mixed — public reads, patient-only writes.
                 */
                .route(RouteConstants.ROUTE_FEEDBACK_SERVICE, r -> r
                        .path(PathConstants.FEEDBACK_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(publicRateLimiter())
                                        .setKeyResolver(ipKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_FEEDBACK)
                                        .setFallbackUri(RouteConstants.FALLBACK_FEEDBACK)
                                )
                                .retry(c -> c
                                        .setRetries(3)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_FEEDBACK)
                )

                /*
                 * admin-service
                 */
                .route(RouteConstants.ROUTE_ADMIN_SERVICE, r -> r
                        .path(PathConstants.ADMIN_PATH)
                        .filters(f -> f
                                .addRequestHeader(HeaderConstants.GATEWAY_SOURCE_HEADER, HeaderConstants.GATEWAY_SOURCE_VALUE)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(adminRateLimiter())
                                        .setKeyResolver(userKeyResolver)
                                )
                                .circuitBreaker(c -> c
                                        .setName(CircuitBreakerConstants.CB_ADMIN)
                                        .setFallbackUri(RouteConstants.FALLBACK_ADMIN)
                                )
                                .retry(c -> c
                                        .setRetries(3)
                                        .setStatuses(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
                                )
                        )
                        .uri(RouteConstants.SVC_ADMIN)
                )

                .build();
    }


}

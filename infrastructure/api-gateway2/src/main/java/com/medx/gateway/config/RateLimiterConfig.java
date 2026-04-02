package com.medx.gateway.config;

import com.medx.gateway.constant.GatewayConstant;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Configuration class for rate limiting key resolvers in the API Gateway.
 * This class provides two different strategies for rate limiting:
 * 1. IP-based rate limiting for public/unauthenticated endpoints
 * 2. User-based rate limiting for authenticated endpoints
 * The rate limiting helps protect the system from abuse and ensures fair usage
 * across different clients and users.
 */
@Configuration
public class RateLimiterConfig {

    /*
     * IP-based KeyResolver for public/unauthenticated routes.
     * Purpose:
     * - Extracts the client's IP address from the request's remote address
     * - Groups all requests from the same IP into a single rate limit bucket
     * - Provides basic protection against abuse from individual IP addresses
     *
     * Use Cases:
     * - Public endpoints like /doctors (public doctor listings)
     * - Registration endpoints: /doctors/register, /users/register
     * - Public feedback endpoints: /feedback/doctor/**
     *
     * Fallback Strategy:
     * - Returns "unknown" if IP address cannot be resolved
     * - Ensures rate limiter continues to function without throwing exceptions
     * - All requests with unresolvable IPs share the same "unknown" bucket
     *
     * Note: This is marked as @Primary to be the default resolver when none is specified
     */
    @Primary
    @Bean(GatewayConstant.RATE_LIMITER_KEY_IP)
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = extractClientIp(exchange);
            return Mono.just(clientIp);
        };
    }

    /*
     * User-based KeyResolver for authenticated routes.
     * Purpose:
     * - Extracts user ID from X-User-Id header (set by JwtAuthenticationFilter)
     * - Provides individual rate limit buckets per authenticated user
     * - Offers fairer rate limiting compared to IP-based approach
     * Advantages over IP-based:
     * - Multiple users behind corporate NAT/proxy don't share the same bucket
     * - More granular control over individual user behavior
     * - Better user experience in shared network environments
     * Fallback Strategy:
     * - Falls back to IP-based resolution if X-User-Id header is missing or blank
     * - Defensive programming: should not occur on properly protected routes
     * - JwtAuthenticationFilter should always set X-User-Id for authenticated requests
     * Security Note:
     * - Relies on JwtAuthenticationFilter to validate and set the user ID header
     * - Header tampering is prevented by upstream authentication validation
     */
    @Bean(GatewayConstant.RATE_LIMITER_KEY_USER)
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = extractUserId(exchange);

            // Use user ID if available and valid
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }

            // Fallback to IP-based resolution for defensive programming
            String clientIp = extractClientIp(exchange);
            return Mono.just(clientIp);
        };
    }

    /**
     * Helper method to extract client IP address from the exchange.
     *
     * @param exchange The server web exchange containing request information
     * @return Client IP address or "unknown" if not resolvable
     */
    private String extractClientIp(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    /**
     * Helper method to extract user ID from request headers.
     *
     * @param exchange The server web exchange containing request information
     * @return User ID from X-User-Id header or null if not present
     */
    private String extractUserId(ServerWebExchange exchange) {
        return exchange.getRequest()
            .getHeaders()
            .getFirst(GatewayConstant.USER_ID_HEADER);
    }
}
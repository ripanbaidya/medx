package com.medx.gateway.config;

import com.medx.gateway.constants.HeaderConstants;
import com.medx.gateway.constants.RateLimiterConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Configuration class for rate limiting key resolvers in the API Gateway.
 * This class provides two different strategies for generating rate limiting keys:
 * 1. IP-based resolution for public/unauthenticated endpoints
 * 2. User-based resolution for authenticated endpoints
 * The appropriate key resolver should be selected based on the security requirements
 * of each route to ensure fair and effective rate limiting.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * IP-based KeyResolver for public and unauthenticated routes.
     * This resolver creates rate limiting buckets based on the client's IP address.
     * It's suitable for public endpoints where user identity is not available.
     * Characteristics:
     * - Groups requests by client IP address
     * - Multiple users behind the same NAT/proxy share the same rate limit bucket
     * - Falls back to "unknown" key if the remote address cannot be resolved
     * - Marked as @Primary to serve as the default resolver when no qualifier is specified
     */
    @Primary
    @Bean(RateLimiterConstants.KEY_RESOLVER_IP)
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(extractClientIp(exchange));
    }

    /**
     * User-based KeyResolver for authenticated routes.
     * This resolver creates individual rate limiting buckets for each authenticated user
     * based on the X-User-Id header that is set by the JwtAuthenticationFilter after
     * successful JWT token validation.
     * Characteristics:
     * - Creates separate rate limit buckets per user ID
     * - More granular and fair than IP-based limiting
     * - Prevents rate limit sharing in corporate networks where many users share one IP
     * - Includes defensive fallback to IP-based limiting if user ID is missing
     * Fallback behavior:
     * - If X-User-Id header is missing or blank, falls back to IP-based resolution
     * - This should rarely happen on protected routes as JwtAuthenticationFilter
     * always sets the header after successful authentication
     * Use cases:
     * - Protected API endpoints requiring authentication
     * - User-specific operations
     * - Any route where individual user rate limiting is desired
     */
    @Bean(RateLimiterConstants.KEY_RESOLVER_USER)
    public KeyResolver userKyeResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HeaderConstants.USER_ID_HEADER);
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }

            return Mono.just(extractClientIp(exchange));
        };
    }

    /**
     * Extracts the client IP address from the incoming request.
     * This method attempts to resolve the client's IP address from the request's
     * remote address.
     *
     * @param exchange the ServerWebExchange containing the request information
     * @return the client IP address as a string, or "unknown" if it cannot be determined
     */
    private String extractClientIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse("unknown");
    }
}
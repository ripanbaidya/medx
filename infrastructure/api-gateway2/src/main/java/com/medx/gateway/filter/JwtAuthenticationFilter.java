package com.medx.gateway.filter;

import com.medx.gateway.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JWT Authentication Filter for Spring Cloud Gateway
 * This filter intercepts all incoming requests and performs the following operations:
 * 1. Checks if the request path is public (no authentication required)
 * 2. For protected paths, extracts JWT claims and enriches request headers
 * 3. Adds user information headers (X-User-Id, X-User-Role, X-User-Email) for downstream services
 * The filter runs with high priority (order = -1) to ensure authentication happens early
 * in the filter chain.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // High priority order to ensure this filter runs early in the chain
    private static final int FILTER_ORDER = -1;

    /**
     * Returns the order of this filter in the filter chain.
     *
     * @return filter order (-1 for high priority)
     */
    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    /**
     * Main filter method that processes each incoming request.
     * Flow:
     * 1. Extract request path
     * 2. Check if path is public (bypass authentication)
     * 3. For protected paths, extract JWT claims and enrich headers
     * 4. Continue filter chain with enriched request
     *
     * @param exchange the current server exchange
     * @param chain    the gateway filter chain
     * @return Mono<Void> representing the completion of the filter chain
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String requestPath = exchange.getRequest().getPath().toString();

        // Allow public paths to bypass authentication
        if (isPublicPath(requestPath)) {
            log.debug("Allowing public access to path: {}", requestPath);
            return chain.filter(exchange);
        }

        // Process authenticated requests
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(securityContext -> {
                // Check if authentication is JWT-based
                if (securityContext.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    return processJwtAuthentication(exchange, chain, jwtAuth, requestPath);
                }

                // No JWT authentication found, continue without enrichment
                log.debug("No JWT authentication found for path: {}", requestPath);
                return chain.filter(exchange);
            })
            // Handle case where security context is empty
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("Empty security context for path: {}", requestPath);
                return chain.filter(exchange);
            }));
    }

    /**
     * Processes JWT authentication and enriches the request with user information headers.
     *
     * @param exchange    the current server exchange
     * @param chain       the gateway filter chain
     * @param jwtAuth     the JWT authentication token
     * @param requestPath the current request path
     * @return Mono<Void> representing the completion of the filter chain
     */
    private Mono<Void> processJwtAuthentication(ServerWebExchange exchange,
                                                GatewayFilterChain chain,
                                                JwtAuthenticationToken jwtAuth,
                                                String requestPath) {
        final Jwt jwt = (Jwt) jwtAuth.getPrincipal();

        // Extract user information from JWT claims
        final String userId = Optional.ofNullable(jwt.getSubject()).orElse("");
        final String email = Optional.ofNullable(jwt.getClaimAsString("email")).orElse("");
        final String role = Optional.ofNullable(extractPrimaryRole(jwt)).orElse("");

        log.debug("Processing JWT authentication - userId: {}, role: {}, path: {}",
            userId, role, requestPath);

        // Create enriched request with user information headers
        // These headers are consumed by downstream services for authorization and user context
        final ServerHttpRequest enrichedRequest = exchange.getRequest().mutate()
            .header(GatewayConstant.USER_ID_HEADER, userId)
            .header(GatewayConstant.USER_ROLE_HEADER, role)
            .header(GatewayConstant.USER_EMAIL_HEADER, email)
            .build();

        // Continue filter chain with enriched request
        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    /**
     * Extracts the primary role from JWT claims based on role hierarchy.
     * Role Priority (highest to lowest):
     * 1. ADMIN - System administrators with full access
     * 2. DOCTOR - Medical professionals with patient access
     * 3. PATIENT - End users with limited access
     *
     * @param jwt the JWT token containing role claims
     * @return the primary role string, or null if no valid role found
     */
    @SuppressWarnings("unchecked")
    private String extractPrimaryRole(Jwt jwt) {
        try {
            // Extract realm_access claim which contains Keycloak roles
            final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                log.debug("No realm_access claim found in JWT");
                return null;
            }

            // Get roles list from realm_access
            final List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null || roles.isEmpty()) {
                log.debug("No roles found in realm_access claim");
                return null;
            }

            // Return highest priority role based on hierarchy
            if (roles.contains("ADMIN")) {
                return "ADMIN";
            }
            if (roles.contains("DOCTOR")) {
                return "DOCTOR";
            }
            if (roles.contains("PATIENT")) {
                return "PATIENT";
            }

            // No recognized roles found
            log.debug("No recognized roles found in JWT. Available roles: {}", roles);
            return null;

        } catch (Exception e) {
            log.warn("Failed to extract role from JWT claims: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Determines if the given request path is public (no authentication required).
     * This method checks against:
     * 1. Static public paths array from GatewayConstant.PUBLIC_PATHS
     * 2. Dynamic wildcard patterns for specific endpoints
     * Uses AntPathMatcher to support wildcard patterns like /api/v1/public/**
     *
     * @param requestPath the incoming request path to check
     * @return true if the path is public, false if authentication is required
     */
    private boolean isPublicPath(String requestPath) {
        // Check against static public paths array
        final boolean isStaticPublicPath = Arrays.stream(GatewayConstant.PUBLIC_PATHS)
            .anyMatch(publicPath -> PATH_MATCHER.match(publicPath, requestPath));

        if (isStaticPublicPath) {
            log.debug("Path matches static public path: {}", requestPath);
            return true;
        }

        // Check against dynamic wildcard public paths
        final boolean isDynamicPublicPath = PATH_MATCHER.match(GatewayConstant.DOCTOR_PUBLIC_PROFILE_PATH, requestPath)
            || PATH_MATCHER.match(GatewayConstant.DOCTOR_SLOTS_PATH, requestPath)
            || PATH_MATCHER.match(GatewayConstant.FEEDBACK_DOCTOR_PATH, requestPath)
            || PATH_MATCHER.match(GatewayConstant.FEEDBACK_DOCTOR_SUMMARY_PATH, requestPath);

        if (isDynamicPublicPath) {
            log.debug("Path matches dynamic public path pattern: {}", requestPath);
        }

        return isDynamicPublicPath;
    }
}
package com.medx.gateway.filter;

import com.medx.gateway.constants.HeaderConstants;
import com.medx.gateway.constants.PathConstants;
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

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String requestPath = exchange.getRequest().getPath().toString();

        if (isPublicPath(requestPath)) {
            log.debug("Public path — skipping JWT enrichment: {}", requestPath);
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                        return enrichRequestWithJwtClaims(exchange, chain, jwtAuth, requestPath);
                    }
                    log.debug("No JWT authentication in security context for path: {}", requestPath);
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Empty security context for path: {}", requestPath);
                    return chain.filter(exchange);
                }));
    }

    // Helpers

    private Mono<Void> enrichRequestWithJwtClaims(ServerWebExchange exchange,
                                                  GatewayFilterChain chain,
                                                  JwtAuthenticationToken jwtAuth,
                                                  String requestPath) {
        Jwt jwt = (Jwt) jwtAuth.getPrincipal();

        String userId = Optional.ofNullable(jwt.getSubject()).orElse("");
        String email = Optional.ofNullable(jwt.getClaimAsString("email")).orElse("");
        String role = Optional.ofNullable(extractPrimaryRole(jwt)).orElse("");

        log.debug("JWT enrichment — userId: {}, role: {}, path: {}", userId, role, requestPath);

        ServerHttpRequest enrichedRequest = exchange.getRequest().mutate()
                .header(HeaderConstants.USER_ID_HEADER, userId)
                .header(HeaderConstants.USER_ROLE_HEADER, role)
                .header(HeaderConstants.USER_EMAIL_HEADER, email)
                .build();

        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    /*
     * Role hierarchy: ADMIN > DOCTOR > PATIENT
     * Only the highest role is forwarded downstream to keep it simple.
     * Keycloak stores roles in realm_access.roles — not in standard "scope" claim.
     */
    @SuppressWarnings("unchecked")
    private String extractPrimaryRole(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return null;

            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null || roles.isEmpty()) return null;

            if (roles.contains("ADMIN")) return "ADMIN";
            if (roles.contains("DOCTOR")) return "DOCTOR";
            if (roles.contains("PATIENT")) return "PATIENT";

            log.debug("No recognized role found. Available: {}", roles);
            return null;

        } catch (Exception e) {
            log.warn("Failed to extract role from JWT: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPublicPath(String requestPath) {
        boolean staticMatch = Arrays.stream(PathConstants.PUBLIC_PATHS)
                .anyMatch(p -> PATH_MATCHER.match(p, requestPath));

        if (staticMatch) return true;

        return PATH_MATCHER.match(PathConstants.DOCTOR_PUBLIC_PROFILE_PATH, requestPath)
                || PATH_MATCHER.match(PathConstants.DOCTOR_SLOTS_PATH, requestPath)
                || PATH_MATCHER.match(PathConstants.FEEDBACK_DOCTOR_PATH, requestPath)
                || PATH_MATCHER.match(PathConstants.FEEDBACK_DOCTOR_SUMMARY_PATH, requestPath);
    }
}

package com.medx.gateway.config;

import com.medx.gateway.constant.GatewayConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Security configuration for the API Gateway using Spring Security WebFlux.
 * This configuration sets up WebFlux security with JWT authentication using Keycloak
 * as the OAuth2 resource server.
 * It defines public endpoints that don't require authentication and configures JWT token
 * processing for protected endpoints.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for the reactive web application.
     *
     * @param http ServerHttpSecurity configuration object
     * @return SecurityWebFilterChain configured security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF protection - not needed for stateless JWT authentication
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Disable form-based login - using JWT tokens instead
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // Disable HTTP Basic authentication - using JWT Bearer tokens instead
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .authorizeExchange(exchange -> exchange
                // Public API endpoints - no authentication required
                .pathMatchers(GatewayConstant.PUBLIC_PATHS).permitAll()

                // Public doctor browsing endpoints - allow anonymous access for discovery
                .pathMatchers(
                    GatewayConstant.DOCTOR_PUBLIC_PROFILE_PATH,
                    GatewayConstant.DOCTOR_SLOTS_PATH
                ).permitAll()

                // Public feedback reading endpoints - allow anonymous access to reviews
                .pathMatchers(
                    GatewayConstant.FEEDBACK_DOCTOR_PATH,
                    GatewayConstant.FEEDBACK_DOCTOR_SUMMARY_PATH
                ).permitAll()

                // All other endpoints require valid JWT authentication
                .anyExchange().authenticated()
            )
            // Configure OAuth2 resource server with JWT support
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Use custom JWT authentication converter for Keycloak token format
                    .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                )
            )
            .build();
    }

    /**
     * Creates a JWT authentication converter specifically configured for Keycloak tokens.
     * Keycloak JWT token structure:
     * <pre>
     * {
     *   "sub": "user-uuid",
     *   "email": "john@example.com",
     *   "realm_access": {
     *     "roles": ["PATIENT", "DOCTOR", "ADMIN", "offline_access", "uma_authorization"]
     *   }
     * }
     * </pre>
     * Standard Spring Security expects roles in "scope" or "scp" claims, but Keycloak stores
     * them in "realm_access.roles". This converter bridges that gap.
     * The converter also adds the "ROLE_" prefix to each role, which is the Spring Security
     * convention that allows using hasRole("PATIENT") instead of hasAuthority("ROLE_PATIENT").
     *
     * @return Converter that transforms Keycloak JWT tokens into Spring Security authentication tokens
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakJwtAuthenticationConverter() {
        // Create authorities converter to extract roles from JWT claims
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Configure to look for roles in Keycloak's "realm_access.roles" claim path
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        // Add "ROLE_" prefix to match Spring Security conventions
        // This allows using @PreAuthorize("hasRole('PATIENT')") instead of hasAuthority('ROLE_PATIENT')
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        // Create JWT authentication converter and configure it with our authorities converter
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        // Wrap in reactive adapter for compatibility with WebFlux reactive streams
        // This is required because we're using Spring WebFlux instead of traditional servlet stack
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}
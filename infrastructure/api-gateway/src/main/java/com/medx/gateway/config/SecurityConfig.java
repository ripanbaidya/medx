package com.medx.gateway.config;

import com.medx.gateway.constants.PathConstants;
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

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Public endpoints that do not require authentication
                        .pathMatchers(PathConstants.PUBLIC_PATHS).permitAll()

                        // Public doctor browsing
                        .pathMatchers(
                                PathConstants.DOCTOR_PUBLIC_PROFILE_PATH,
                                PathConstants.DOCTOR_SLOTS_PATH
                        ).permitAll()

                        // Public feedback reading
                        .pathMatchers(
                                PathConstants.FEEDBACK_DOCTOR_PATH,
                                PathConstants.FEEDBACK_DOCTOR_SUMMARY_PATH
                        ).permitAll()

                        // Everything else requires valid JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * Keycloak JWT structures:
     * <pre>
     *     {
     *         "sub": "userId",
     *         "email": "your.email@example.com"
     *         "realm_access" {
     *             "roles": [ "PATIENT", "DOCTOR", "ADMIN" ]
     *         }
     *     }
     * </pre>
     * Standard spring security looks for roles in "scope"/"scp", but shis coverter
     * bridges that gap - reads from "realm_access.roles" and adds "ROLE_" prefix fpr
     * hasRole() compatibility.
     *
     * @return a Converter that transforms a Keycloak JWT into an AbstractAuthenticationToken
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakJwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}

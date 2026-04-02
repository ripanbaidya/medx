package com.medx.gateway.filter;

import com.medx.gateway.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Global filter that ensures every request has a correlation ID for distributed tracing.
 * This filter runs early in the filter chain to ensure all subsequent filters and services
 * can use the correlation ID for logging and tracing purposes.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    /**
     * Order -2 — runs before JwtAuthenticationFilter (order -1).
     * Correlation ID should be the very first thing attached so all subsequent logs
     * (including JWT filter logs) carry it for proper request tracing.
     */
    @Override
    public int getOrder() {
        return -2;
    }

    /**
     * Filters incoming requests to ensure they have correlation ID and request timestamp headers.
     * If a correlation ID is already present in the request headers, it will be reused to maintain
     * tracing continuity. If not present, a new UUID will be generated.
     *
     * @param exchange the current server exchange containing request/response
     * @param chain    the filter chain to continue processing
     * @return Mono<Void> representing the completion of the filter processing
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract existing correlation ID from request headers if present
        String correlationId = request.getHeaders().getFirst(GatewayConstant.CORRELATION_ID_HEADER);

        // Generate correlation ID if not provided by client to ensure every request is traceable
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {} for request path: {}", correlationId, request.getPath());
        } else {
            log.debug("Reusing existing correlation ID: {} for request path: {}", correlationId, request.getPath());
        }

        // Capture request timestamp for performance monitoring and audit purposes
        String requestTime = String.valueOf(Instant.now().toEpochMilli());
        final String finalCorrelationId = correlationId;

        log.debug("Processing request with correlationId: {}, path: {}, method: {}", finalCorrelationId,
            request.getPath(), request.getMethod());

        // Create mutated request with required headers for downstream services
        // This ensures all microservices receive the correlation ID and request timestamp
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(GatewayConstant.CORRELATION_ID_HEADER, finalCorrelationId)
            .header(GatewayConstant.REQUEST_TIME_HEADER, requestTime)
            .build();

        // Continue filter chain with the enhanced request containing tracing headers
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
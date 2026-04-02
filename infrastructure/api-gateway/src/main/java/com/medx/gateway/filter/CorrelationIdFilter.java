package com.medx.gateway.filter;

import com.medx.gateway.constants.HeaderConstants;
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

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = request.getHeaders().getFirst(HeaderConstants.CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("No correlation ID found in request. Generated new ID: {}", correlationId);
        } else {
            log.debug("Reusing Correlation ID: {} for path: {}", correlationId, request.getPath());
        }

        String requestTime = String.valueOf(Instant.now().toEpochMilli());
        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HeaderConstants.CORRELATION_ID_HEADER, finalCorrelationId)
                .header(HeaderConstants.REQUEST_TIME_HEADER, requestTime)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Order -2, runs before {@link JwtAuthenticationFilter} which has {@code order -1}.
     * correlation ID must be attached first so all subsequence filter logs carry it.
     *
     * @return the order of this filter
     */
    @Override
    public int getOrder() {
        return -2;
    }
}

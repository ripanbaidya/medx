package com.medx.gateway.controller;

import com.medx.commons.enums.ErrorCode;
import com.medx.commons.response.error.ErrorDetail;
import com.medx.gateway.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Fallback response
 *
 * <pre>
 * {
 *   "type":          "GATEWAY",
 *   "code":          "APPOINTMENT_SERVICE_UNAVAILABLE",
 *   "message":       "Appointment service is currently unavailable. Please try again shortly.",
 *   "status":        503,
 *   "service":       "api-gateway",
 *   "correlationId": "3f6a1b2c-9d4e-4f8a-b1c2-d3e4f5a6b7c8",
 *   "path":          "/api/v1/appointments",
 *   "timestamp":     "2025-04-01T10:00:00Z"
 * }
 * </pre>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user-service")
    public Mono<ResponseEntity<ErrorDetail>> userServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.USER_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/doctor-service")
    public Mono<ResponseEntity<ErrorDetail>> doctorServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/appointment-service")
    public Mono<ResponseEntity<ErrorDetail>> appointmentServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.APPOINTMENT_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/payment-service")
    public Mono<ResponseEntity<ErrorDetail>> paymentServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/notification-service")
    public Mono<ResponseEntity<ErrorDetail>> notificationServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.NOTIFICATION_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/feedback-service")
    public Mono<ResponseEntity<ErrorDetail>> feedbackServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.FEEDBACK_SERVICE_UNAVAILABLE, request);
    }

    @GetMapping("/admin-service")
    public Mono<ResponseEntity<ErrorDetail>> adminServiceFallback(ServerHttpRequest request) {
        return buildFallback(ErrorCode.ADMIN_SERVICE_UNAVAILABLE, request);
    }

    /**
     * Builds ErrorDetail fallback response.
     * correlationId is extracted from the request header, {@code CorrelationIdFilter} already
     * set it on the way in.
     * path is extracted from the original request URI so the error response shows the actual
     * path the client hit, not the internal /fallback/* path.
     */
    private Mono<ResponseEntity<ErrorDetail>> buildFallback(ErrorCode errorCode,
                                                            ServerHttpRequest request) {

        final String correlationId = request.getHeaders().getFirst(GatewayConstant.CORRELATION_ID_HEADER);
        final String path = request.getHeaders().getFirst(GatewayConstant.FORWARDED_PATH) != null
            ? request.getHeaders().getFirst(GatewayConstant.FORWARDED_PATH)
            : request.getURI().getPath();

        log.warn("Circuit breaker fallback triggered — code: {}, correlationId: {}, path: {}",
            errorCode.name(), correlationId, path);

        var error = ErrorDetail.builder()
            .code(errorCode)
            .service(GatewayConstant.SERVICE_NAME)
            .correlationId(correlationId)
            .path(path)
            .build();

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(error)
        );
    }
}

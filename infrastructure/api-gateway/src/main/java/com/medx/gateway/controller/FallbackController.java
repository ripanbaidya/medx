package com.medx.gateway.controller;

import com.medx.gateway.constants.HeaderConstants;
import com.medx.gateway.constants.RouteConstants;
import com.medx.gateway.dto.FallbackResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user-service")
    public Mono<ResponseEntity<FallbackResponse>> userServiceFallback(ServerHttpRequest request) {
        return buildFallback("USER_SERVICE_UNAVAILABLE",
                "User service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/doctor-service")
    public Mono<ResponseEntity<FallbackResponse>> doctorServiceFallback(ServerHttpRequest request) {
        return buildFallback("DOCTOR_SERVICE_UNAVAILABLE",
                "Doctor service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/appointment-service")
    public Mono<ResponseEntity<FallbackResponse>> appointmentServiceFallback(ServerHttpRequest request) {
        return buildFallback("APPOINTMENT_SERVICE_UNAVAILABLE",
                "Appointment service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/payment-service")
    public Mono<ResponseEntity<FallbackResponse>> paymentServiceFallback(ServerHttpRequest request) {
        return buildFallback("PAYMENT_SERVICE_UNAVAILABLE",
                "Payment service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/notification-service")
    public Mono<ResponseEntity<FallbackResponse>> notificationServiceFallback(ServerHttpRequest request) {
        return buildFallback("NOTIFICATION_SERVICE_UNAVAILABLE",
                "Notification service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/feedback-service")
    public Mono<ResponseEntity<FallbackResponse>> feedbackServiceFallback(ServerHttpRequest request) {
        return buildFallback("FEEDBACK_SERVICE_UNAVAILABLE",
                "Feedback service is currently unavailable. Please try again shortly.", request);
    }

    @GetMapping("/admin-service")
    public Mono<ResponseEntity<FallbackResponse>> adminServiceFallback(ServerHttpRequest request) {
        return buildFallback("ADMIN_SERVICE_UNAVAILABLE",
                "Admin service is currently unavailable. Please try again shortly.", request);
    }

    /*
     * correlationId — already set by CorrelationIdFilter on the way in.
     * path — prefer X-Forwarded-Path (original client path) over /fallback/* path.
     */
    private Mono<ResponseEntity<FallbackResponse>> buildFallback(String code, String message,
                                                                 ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(HeaderConstants.CORRELATION_ID_HEADER);
        String path = request.getHeaders().getFirst(HeaderConstants.FORWARDED_PATH_HEADER);
        if (path == null || path.isBlank()) {
            path = request.getURI().getPath();
        }

        log.warn("Circuit breaker fallback — code: {}, correlationId: {}, path: {}", code, correlationId, path);

        FallbackResponse response = FallbackResponse.of(code, message,
                RouteConstants.SERVICE_NAME, correlationId, path);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
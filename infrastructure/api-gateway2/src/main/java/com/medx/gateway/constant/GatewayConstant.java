package com.medx.gateway.constant;

public final class GatewayConstant {

    private GatewayConstant() {
    }

    // API Version prefix for all routes
    public static final String API_PREFIX = "/api/v1";

    // Route IDs: These are Unique identifier for each route in the gateway config.
    // Used in circuit breaker naming and logging.
    public static final String ROUTE_USER_SERVICE = "user-service";
    public static final String ROUTE_DOCTOR_SERVICE = "doctor-service";
    public static final String ROUTE_APPOINTMENT_SERVICE = "appointment-service";
    public static final String ROUTE_PAYMENT_SERVICE = "payment-service";
    public static final String ROUTE_NOTIFICATION_SERVICE = "notification-service";
    public static final String ROUTE_FEEDBACK_SERVICE = "feedback-service";
    public static final String ROUTE_ADMIN_SERVICE = "admin-service";

    // Downstream Service URIs (Eureka load-balanced)
    // lb:// tells Spring Cloud Gateway to resolve via Eureka.
    // Make sure the name is exactly similar to spring.application.name of each service.
    public static final String SVC_USER = "lb://user-service";
    public static final String SVC_DOCTOR = "lb://doctor-service";
    public static final String SVC_APPOINTMENT = "lb://appointment-service";
    public static final String SVC_PAYMENT = "lb://payment-service";
    public static final String SVC_NOTIFICATION = "lb://notification-service";
    public static final String SVC_FEEDBACK = "lb://feedback-service";
    public static final String SVC_ADMIN = "lb://admin-service";

    // Path Patterns: These are the incoming path patterns the gateway matches.
    // Here, ** means match anything after the prefix.
    public static final String USER_PATH = API_PREFIX + "/users/**";
    public static final String DOCTOR_PATH = API_PREFIX + "/doctors/**";
    public static final String APPOINTMENT_PATH = API_PREFIX + "/appointments/**";
    public static final String PAYMENT_PATH = API_PREFIX + "/payments/**";
    public static final String NOTIFICATION_PATH = API_PREFIX + "/notifications/**";
    public static final String FEEDBACK_PATH = API_PREFIX + "/feedback/**";
    public static final String ADMIN_PATH = API_PREFIX + "/admin/**";

    // WebSocket Path: Separate constant for WS upgrade path in notification service.
    public static final String NOTIFICATION_WS_PATH = "/ws/notifications/**";

    // Fallback URIs: When circuit breaker opens, gateway forwards to these local fallback
    // endpoints inside the gateway itself.
    // forward:/ means it calls a local controller endpoint.
    public static final String FALLBACK_USER = "forward:/fallback/user-service";
    public static final String FALLBACK_DOCTOR = "forward:/fallback/doctor-service";
    public static final String FALLBACK_APPOINTMENT = "forward:/fallback/appointment-service";
    public static final String FALLBACK_PAYMENT = "forward:/fallback/payment-service";
    public static final String FALLBACK_NOTIFICATION = "forward:/fallback/notification-service";
    public static final String FALLBACK_FEEDBACK = "forward:/fallback/feedback-service";
    public static final String FALLBACK_ADMIN = "forward:/fallback/admin-service";

    // Request Headers — added by gateway to every downstream call

    // Tells downstream service this request came through the gateway.
    // Services can reject direct calls that don't have this header.
    public static final String GATEWAY_SOURCE_HEADER = "X-Gateway-Source";
    public static final String GATEWAY_SOURCE_VALUE = "api-gateway";

    // Unique ID per request — propagated across all services for tracing.
    // If client sends one → reuse it. If not → gateway generates UUID.
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String FORWARDED_PATH = "X-Forwarded-Path";

    // Timestamp when request entered the gateway (epoch ms).
    // Useful for measuring total request latency end to end.
    public static final String REQUEST_TIME_HEADER = "X-Request-Time";

    // JWT Enrichment Headers
    // After Spring Security validates the JWT, our filter extracts these claims and passes them
    // as headers to downstream services.
    // Downstream services never parse JWT themselves — they trust these gateway-set headers only

    // Keycloak subject claim (sub) — unique user ID
    public static final String USER_ID_HEADER = "X-User-Id";

    // Role extracted from realm_access.roles in JWT
    public static final String USER_ROLE_HEADER = "X-User-Role";

    // Email extracted from JWT claims
    public static final String USER_EMAIL_HEADER = "X-User-Email";

    // Public Paths: Used in SecurityConfig to permit without authentication.
    public static final String[] PUBLIC_PATHS = {
        // Users registration
        API_PREFIX + "/users/register",

        // Doctor registration and public profile browsing
        API_PREFIX + "/doctors/register",
        API_PREFIX + "/doctors",
        API_PREFIX + "/doctors/departments",

        // Payment webhooks — called by Razorpay/Stripe, no user token
        API_PREFIX + "/payments/razorpay/webhook",
        API_PREFIX + "/payments/stripe/webhook",

        // Public feedback reading
        API_PREFIX + "/feedback/platform",

        // Actuator health for load balancer / k8s probes
        "/actuator/health",
        "/actuator/info",

        // Gateway fallback endpoints — must be accessible internally
        "/fallback/**"
    };

    // Path Patterns with wildcards — for SecurityConfig
    // These need separate treatment because they have path variables.

    // GET /api/v1/doctors/{doctorId} — public doctor profile
    public static final String DOCTOR_PUBLIC_PROFILE_PATH = API_PREFIX + "/doctors/{doctorId}";

    // GET /api/v1/doctors/{doctorId}/slots — public slot viewing
    public static final String DOCTOR_SLOTS_PATH = API_PREFIX + "/doctors/{doctorId}/slots";

    // GET /api/v1/feedback/doctor/{doctorId} — public doctor feedback
    public static final String FEEDBACK_DOCTOR_PATH = API_PREFIX + "/feedback/doctor/**";

    // GET /api/v1/feedback/doctor/{doctorId}/summary — public rating summary
    public static final String FEEDBACK_DOCTOR_SUMMARY_PATH = API_PREFIX + "/feedback/doctor/**/summary";

    // Rate Limiter Keys
    // Used as bean names for KeyResolver in RateLimiterConfig.

    // For unauthenticated/public routes — limit by client IP
    public static final String RATE_LIMITER_KEY_IP = "ipKeyResolver";

    // For authenticated routes — limit by user ID from JWT
    public static final String RATE_LIMITER_KEY_USER = "userKeyResolver";

    // Circuit Breaker Names
    // Must match Resilience4j config in application.yml.
    // Named per service for independent thresholds and monitoring.
    public static final String CB_USER = "userServiceCB";
    public static final String CB_DOCTOR = "doctorServiceCB";
    public static final String CB_APPOINTMENT = "appointmentServiceCB";
    public static final String CB_PAYMENT = "paymentServiceCB";
    public static final String CB_NOTIFICATION = "notificationServiceCB";
    public static final String CB_FEEDBACK = "feedbackServiceCB";
    public static final String CB_ADMIN = "adminServiceCB";

    // Service name — used in ErrorDetail.service field
    // Identifies which service produced the error response
    public static final String SERVICE_NAME = "api-gateway";

}
package com.medx.gateway.constants;

public final class PathConstants {

    private PathConstants() {
    }

    private static final String API_PREFIX = RouteConstants.API_PREFIX;

    // Route path patterns (** = match everything after prefix)
    public static final String USER_PATH = API_PREFIX + "/users/**";
    public static final String DOCTOR_PATH = API_PREFIX + "/doctors/**";
    public static final String APPOINTMENT_PATH = API_PREFIX + "/appointments/**";
    public static final String PAYMENT_PATH = API_PREFIX + "/payments/**";
    public static final String NOTIFICATION_PATH = API_PREFIX + "/notifications/**";
    public static final String FEEDBACK_PATH = API_PREFIX + "/feedback/**";
    public static final String ADMIN_PATH = API_PREFIX + "/admin/**";

    // WebSocket path — separate route for WS upgrade
    public static final String NOTIFICATION_WS_PATH = "/ws/notifications/**";

    // Public paths with path variables (need AntPathMatcher for JWT filter)
    public static final String DOCTOR_PUBLIC_PROFILE_PATH = API_PREFIX + "/doctors/{doctorId}";
    public static final String DOCTOR_SLOTS_PATH = API_PREFIX + "/doctors/{doctorId}/slots";
    public static final String FEEDBACK_DOCTOR_PATH = API_PREFIX + "/feedback/doctor/**";
    public static final String FEEDBACK_DOCTOR_SUMMARY_PATH = API_PREFIX + "/feedback/doctor/*/summary";

    // Static public paths — no authentication required
    // Used in SecurityConfig.permitAll() and JwtAuthenticationFilter
    public static final String[] PUBLIC_PATHS = {
            API_PREFIX + "/users/register",
            API_PREFIX + "/doctors/register",
            API_PREFIX + "/doctors",
            API_PREFIX + "/doctors/departments",
            API_PREFIX + "/payments/razorpay/webhook",
            API_PREFIX + "/payments/stripe/webhook",
            API_PREFIX + "/feedback/platform",
            "/actuator/health",
            "/actuator/info",
            "/fallback/**",
            "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };
}
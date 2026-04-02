package com.medx.gateway.constants;

public final class RouteConstants {

    private RouteConstants() {
    }

    public static final String API_PREFIX = "/api/v1";

    // Route IDs
    public static final String ROUTE_USER_SERVICE = "user-service";
    public static final String ROUTE_DOCTOR_SERVICE = "doctor-service";
    public static final String ROUTE_APPOINTMENT_SERVICE = "appointment-service";
    public static final String ROUTE_PAYMENT_SERVICE = "payment-service";
    public static final String ROUTE_NOTIFICATION_SERVICE = "notification-service";
    public static final String ROUTE_FEEDBACK_SERVICE = "feedback-service";
    public static final String ROUTE_ADMIN_SERVICE = "admin-service";

    // Downstream URIs (Eureka load-balanced)
    public static final String SVC_USER = "lb://user-service";
    public static final String SVC_DOCTOR = "lb://doctor-service";
    public static final String SVC_APPOINTMENT = "lb://appointment-service";
    public static final String SVC_PAYMENT = "lb://payment-service";
    public static final String SVC_NOTIFICATION = "lb://notification-service";
    public static final String SVC_FEEDBACK = "lb://feedback-service";
    public static final String SVC_ADMIN = "lb://admin-service";

    // Fallback URIs (forward to local FallbackController)
    public static final String FALLBACK_USER = "forward:/fallback/user-service";
    public static final String FALLBACK_DOCTOR = "forward:/fallback/doctor-service";
    public static final String FALLBACK_APPOINTMENT = "forward:/fallback/appointment-service";
    public static final String FALLBACK_PAYMENT = "forward:/fallback/payment-service";
    public static final String FALLBACK_NOTIFICATION = "forward:/fallback/notification-service";
    public static final String FALLBACK_FEEDBACK = "forward:/fallback/feedback-service";
    public static final String FALLBACK_ADMIN = "forward:/fallback/admin-service";

    // Gateway identity
    public static final String SERVICE_NAME = "api-gateway";
}
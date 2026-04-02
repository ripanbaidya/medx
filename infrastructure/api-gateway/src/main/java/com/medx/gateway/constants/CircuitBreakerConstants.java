package com.medx.gateway.constants;

public final class CircuitBreakerConstants {

    private CircuitBreakerConstants() {
    }

    // Circuit breaker names — must match resilience4j.circuitbreaker.instances keys in yml
    public static final String CB_USER = "userServiceCB";
    public static final String CB_DOCTOR = "doctorServiceCB";
    public static final String CB_APPOINTMENT = "appointmentServiceCB";
    public static final String CB_PAYMENT = "paymentServiceCB";
    public static final String CB_NOTIFICATION = "notificationServiceCB";
    public static final String CB_FEEDBACK = "feedbackServiceCB";
    public static final String CB_ADMIN = "adminServiceCB";
}
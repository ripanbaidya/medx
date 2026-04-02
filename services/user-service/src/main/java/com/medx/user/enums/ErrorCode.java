package com.medx.user.enums;

import com.medx.commons.enums.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // VALIDATION errors

    VALIDATION_FAILED(ErrorType.VALIDATION, "One or more fields failed validation"),
    MISSING_REQUIRED_FIELD(ErrorType.VALIDATION, "A required field is missing"),
    INVALID_FORMAT(ErrorType.VALIDATION, "Field format is invalid"),

    // AUTHENTICATION errors

    TOKEN_MISSING(ErrorType.AUTHENTICATION, "Authorization token is missing"),
    TOKEN_EXPIRED(ErrorType.AUTHENTICATION, "Authorization token has expired"),
    TOKEN_INVALID(ErrorType.AUTHENTICATION, "Authorization token is invalid"),

    // AUTHORIZATION errors

    ACCESS_DENIED(ErrorType.AUTHORIZATION, "You do not have permission to perform this action"),

    // NOT_FOUND errors

    USER_NOT_FOUND(ErrorType.NOT_FOUND, "User not found"),
    DOCTOR_NOT_FOUND(ErrorType.NOT_FOUND, "Doctor not found"),
    APPOINTMENT_NOT_FOUND(ErrorType.NOT_FOUND, "Appointment not found"),
    PAYMENT_NOT_FOUND(ErrorType.NOT_FOUND, "Payment record not found"),
    SLOT_NOT_FOUND(ErrorType.NOT_FOUND, "Requested slot not found"),
    FEEDBACK_NOT_FOUND(ErrorType.NOT_FOUND, "Feedback not found"),

    // CONFLICT errors

    SLOT_ALREADY_BOOKED(ErrorType.CONFLICT, "This slot is already booked"),
    SLOT_LOCK_CONFLICT(ErrorType.CONFLICT, "Another booking is in progress for this slot. Please try again shortly."),
    USER_ALREADY_EXISTS(ErrorType.CONFLICT, "An account with this email already exists"),
    DOCTOR_ALREADY_EXISTS(ErrorType.CONFLICT, "A doctor profile with this email already exists"),
    FEEDBACK_ALREADY_SUBMITTED(ErrorType.CONFLICT, "You have already submitted feedback for this appointment"),

    // BUSINESS errors

    APPOINTMENT_NOT_CANCELLABLE(ErrorType.BUSINESS, "This appointment cannot be cancelled at this stage"),
    PAYMENT_ALREADY_COMPLETED(ErrorType.BUSINESS, "Payment has already been completed for this appointment"),
    FEEDBACK_EDIT_WINDOW_EXPIRED(ErrorType.BUSINESS, "Feedback can only be edited within 24 hours of submission"),
    DOCTOR_NOT_VERIFIED(ErrorType.BUSINESS, "Doctor profile is pending verification"),
    INVALID_PAYMENT_GATEWAY(ErrorType.BUSINESS, "Selected payment gateway is not supported"),

    // GATEWAY errors — circuit breaker fallback
    // One per downstream service so frontend/logs
    // can identify exactly which service is down.
    USER_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "User service is currently unavailable. Please try again shortly."),
    DOCTOR_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Doctor service is currently unavailable. Please try again shortly."),
    APPOINTMENT_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Appointment service is currently unavailable. Please try again shortly."),
    PAYMENT_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Payment service is currently unavailable. Please try again shortly."),
    NOTIFICATION_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Notification service is currently unavailable. Please try again shortly."),
    FEEDBACK_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Feedback service is currently unavailable. Please try again shortly."),
    ADMIN_SERVICE_UNAVAILABLE(ErrorType.GATEWAY, "Admin service is currently unavailable. Please try again shortly."),

    // SYSTEM errors

    INTERNAL_SERVER_ERROR(ErrorType.SYSTEM, "An unexpected error occurred. Please try again later."),
    SERVICE_COMMUNICATION_FAILURE(ErrorType.SYSTEM, "Failed to communicate with a downstream service");

    private final ErrorType type;
    private final String defaultMessage;
}
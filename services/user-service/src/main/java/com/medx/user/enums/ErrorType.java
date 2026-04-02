package com.medx.commons.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of standardized error types
 * Each error type maps to an appropriate HTTP status code and provides
 * a descriptive title for consistent error handling across services.
 */
@Getter
@AllArgsConstructor
public enum ErrorType {

    /**
     * VALIDATION — Bad input from client.
     * Missing fields, constraint violations, type mismatches.
     * <br>{@code HTTP 400 - Bad Request}
     */
    VALIDATION("Validation Failed", HttpStatus.BAD_REQUEST.value()),

    /**
     * AUTHENTICATION — Missing or invalid JWT token.
     * <br>{@code HTTP 401 - Unauthorized}
     */
    AUTHENTICATION("Authentication Required or Invalid", HttpStatus.UNAUTHORIZED.value()),

    /**
     * AUTHORIZATION — Valid token but insufficient role/permission.
     * <br>{@code HTTP 403 - Forbidden}
     */
    AUTHORIZATION("Access Denied", HttpStatus.FORBIDDEN.value()),

    /**
     * NOT_FOUND — Requested resource does not exist.
     * <br>{@code HTTP 404 - Not Found}
     */
    NOT_FOUND("Resource Not Found", HttpStatus.NOT_FOUND.value()),

    /**
     * CONFLICT — Resource state conflict.
     * Duplicate registration, concurrent slot booking collision.
     * <br>{@code HTTP 409 - Conflict}
     */
    CONFLICT("Resource Conflict", HttpStatus.CONFLICT.value()),

    /**
     * BUSINESS — Request is valid but violates business rules.
     * Slot already booked, appointment not cancellable, etc.
     * <br>{@code HTTP 422 - Unprocessable Entity}
     */
    BUSINESS("Business Rule Violation", HttpStatus.UNPROCESSABLE_ENTITY.value()),

    /**
     * GATEWAY — Circuit breaker open, downstream service unreachable.
     * Returned by api-gateway fallback endpoints.
     * <br>{@code HTTP 503 - Service Unavailable}
     */
    GATEWAY("Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE.value()),

    /**
     * SYSTEM — Unexpected internal server error.
     * Unhandled exceptions, infrastructure failures.
     * <br>{@code HTTP 500 - Internal Server Error}
     */
    SYSTEM("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String title;
    private final int statusCode;

    /**
     * Retrieves the HttpStatus object corresponding to this error type.
     *
     * @return HttpStatus enum value matching the status code
     */
    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(this.statusCode);
    }

    /**
     * Checks if this error type represents a client error (4xx status codes).
     *
     * @return true if status code is in the 400-499 range
     */
    public boolean isClientError() {
        return this.statusCode >= 400 && this.statusCode < 500;
    }

    /**
     * Checks if this error type represents a server error (5xx status codes).
     *
     * @return true if status code is in the 500-599 range
     */
    public boolean isServerError() {
        return this.statusCode >= 500 && this.statusCode < 600;
    }
}
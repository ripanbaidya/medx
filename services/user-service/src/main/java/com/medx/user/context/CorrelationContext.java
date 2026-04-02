package com.medx.user.context;

/**
 * Thread-local holder for the correlation ID propagated by the API Gateway.
 */
public final class CorrelationContext {

    private CorrelationContext() {
    }

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    /**
     * Stores the correlation ID for the current request thread.
     * Called by each service's incoming request filter/interceptor.
     *
     * @param correlationId the X-Correlation-Id header value from the gateway
     */
    public static void set(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    /**
     * Retrieves the correlation ID for the current thread.
     * Returns empty string if not set — safe default, never throws.
     *
     * @return correlation ID or empty string
     */
    public static String get() {
        String id = CORRELATION_ID.get();
        return id != null ? id : "";
    }

    /**
     * Clears the correlation ID from the current thread.
     * MUST be called after each request completes to prevent ThreadLocal leaks
     * in thread-pool environments (Tomcat reuses threads).
     */
    public static void clear() {
        CORRELATION_ID.remove();
    }
}
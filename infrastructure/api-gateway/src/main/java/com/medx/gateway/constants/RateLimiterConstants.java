package com.medx.gateway.constants;

public final class RateLimiterConstants {

    private RateLimiterConstants() {
    }

    // Bean names for KeyResolver — used with @Qualifier in GatewayConfig
    // IP-based: for public/unauthenticated routes
    public static final String KEY_RESOLVER_IP = "ipKeyResolver";

    // User-based: for authenticated routes (per user ID from JWT)
    public static final String KEY_RESOLVER_USER = "userKeyResolver";
}
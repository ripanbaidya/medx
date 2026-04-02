package com.medx.user.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeyCloakAdminProperties (
    String serverUrl,
    String realm,
    String clientId,
    String clientSecret,
    String adminUsername,
    String adminPassword

){
}

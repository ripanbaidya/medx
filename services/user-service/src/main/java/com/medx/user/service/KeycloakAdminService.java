package com.medx.user.service;

import com.medx.user.exception.KeycloakRegistrationException;
import com.medx.user.properties.KeyCloakAdminProperties;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakAdminService {

    private static final String ROLE_PATIENT = "PATIENT";

    private final KeyCloakAdminProperties keyCloakAdminProperties;

    private Keycloak getKeyCloakClient() {
        return KeycloakBuilder.builder()
            .serverUrl(keyCloakAdminProperties.serverUrl())
            .realm("master") // admin always authenticates against master
            .clientId("admin-cli") // admin-cli is keycloak's built-in admin client
            .username(keyCloakAdminProperties.adminUsername())
            .password(keyCloakAdminProperties.adminPassword())
            .build();
    }

    /**
     * Create a new user in keycloak
     */
    public String createUser(String email, String firstName, String lastName,
                             String password) {
        log.info("KeyCloakAdminService - Creating user in Keycloak with email: {}", email);

        // Build user representation with email, name, credentials
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmailVerified(true); // TODO: Will implement email verification later
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        // Set password credential
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        try (Keycloak keycloak = getKeyCloakClient()) {
            RealmResource realmResource = keycloak.realm(keyCloakAdminProperties.realm());
            UsersResource usersResource = realmResource.users();

            Response response = usersResource.create(user);
            int status = response.getStatus();

            if (status == HttpStatus.CONFLICT.value()) {
                // 409 if email/ username already exist
                log.warn("User already exist with email: {}", email);
                throw new KeycloakRegistrationException("An account with this email already exist in keycloak!");
            }
            if (status != HttpStatus.CREATED.value()) {
                // Any other error
                log.error("Error creating user in keycloak. Status: {}, Response: {}", status, response);
                throw new KeycloakRegistrationException("Error creating user in keycloak!, status: " + status);
            }

            // Extract keycloak user UUID from Location header.
            String locationHeader = response.getHeaderString("Location");
            String keyCloakUserId = locationHeader.substring(
                locationHeader.lastIndexOf('/') + 1
            );

            log.info("User created successfully in keycloak: {}", keyCloakUserId);

            assignRole(realmResource, keyCloakUserId, ROLE_PATIENT);

            return keyCloakUserId;
        } catch (KeycloakRegistrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating user in keycloak: {}", e.getMessage(), e);
            throw new KeycloakRegistrationException("Unexpected error during Keycloak user creation", e);
        }
    }

    /**
     * Assign a realm role to keycloak user.
     * Called internally after user creation
     */
    private void assignRole(RealmResource realmResource, String keyCloakUserId, String roleName) {
        try {
            RoleRepresentation role = realmResource.roles()
                .get(roleName)
                .toRepresentation();

            realmResource.users()
                .get(keyCloakUserId)
                .roles()
                .realmLevel()
                .add(List.of(role));

            log.info("Role: {} assigned to user: {}", roleName, keyCloakUserId);

        } catch (Exception e) {
            log.error("KeycloakAdminService — failed to assign role {}: {}", roleName, e.getMessage(), e);
            throw new KeycloakRegistrationException("Failed to assign role " + roleName + " in Keycloak", e);
        }
    }

    /**
     * Change a user's password in keycloak.
     * Note: this will be called from userService.changePassword()
     * Current password is verified by attempting a keycloak token grant before this method is called
     *
     */
    public void updatePassword(String keyCloakUserId, String newPassword) {
        log.info("KeyCloakAdminService - Updating password for user: {}", keyCloakUserId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        try (Keycloak keycloak = getKeyCloakClient()) {
            keycloak.realm(keyCloakAdminProperties.realm())
                .users()
                .get(keyCloakUserId)
                .resetPassword(credential);

            log.info("KeycloakAdminService - Password updated successfully for user: {}", keyCloakUserId);

        } catch (Exception e) {
            log.error("KeycloakAdminService - Password update failed! {}", e.getMessage());
            throw new KeycloakRegistrationException("Failed to update password in Keycloak", e);
        }
    }

    /**
     * Disable a keycloak user - called on account deactivation
     * user record in our DB is soft-deleted, keycloak used is disabled.
     * Disabled keycloak user can't log in or get new tokens
     *
     * @param keycloakUserId the id of the keycloak user to disable
     */
    public void disableUser(String keycloakUserId) {
       log.info("KeycloakAdminService - disabling user: {}", keycloakUserId);

       try (Keycloak keycloak = getKeyCloakClient() ) {
           UserRepresentation user = keycloak.realm(keyCloakAdminProperties.realm())
               .users()
               .get(keycloakUserId)
               .toRepresentation();

           user.setEnabled(false);
           keycloak.realm(keyCloakAdminProperties.realm())
               .users()
               .get(keycloakUserId)
               .update(user);

           log.info("KeycloakAdminService - user disabled: {}", keycloak);

       } catch (Exception e) {
           log.error("KeycloakAdminService — disable user failed: {}", e.getMessage(), e);
           throw new KeycloakRegistrationException("Failed to disable user in Keycloak", e);
       }
    }

    /**
     *
     */
    public boolean verifyCurrentPassword(String email, String currentPassword) {
       try {
           Keycloak testClient = KeycloakBuilder.builder()
               .serverUrl(keyCloakAdminProperties.serverUrl())
               .realm(keyCloakAdminProperties.realm())
               .clientId(keyCloakAdminProperties.clientId())
               .clientSecret(keyCloakAdminProperties.clientSecret())
               .username(email)
               .password(currentPassword)
               .build();

           testClient.tokenManager().getAccessToken();
           testClient.close();

           return true;

       } catch (Exception e) {
           log.warn("KeycloakAdminService — password verification failed for: {}", email);
           return false;
       }
    }
}

package com.medx.user.controller;

import com.medx.user.payload.dto.success.ResponseWrapper;
import com.medx.user.payload.request.ChangePasswordRequest;
import com.medx.user.payload.request.RegisterRequest;
import com.medx.user.payload.request.UpdateProfileRequest;
import com.medx.user.payload.response.UserProfileResponse;
import com.medx.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final String USER_SERVICE_API_PATH = "/api/v1/users";
    private static final String X_USER_ID = "X-User-Id";

    @Value("${spring.application.name}")
    private String serviceName;

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> register(
        @Valid
        @RequestBody RegisterRequest request
    ) {
        String path = USER_SERVICE_API_PATH + "/register";
        var response = userService.register(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ResponseWrapper.created(
                    "User registered Successfully",
                    response, serviceName, path
                )
            );
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> getProfile(
        @RequestHeader(X_USER_ID) String keycloakId
    ) {
        String path = USER_SERVICE_API_PATH + "/profile";
        var response = userService.getProfile(keycloakId);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Profile fetched successfully",
                response, serviceName, path
            )
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> updateProfile(
        @RequestHeader(X_USER_ID) String keycloakId,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        String path = USER_SERVICE_API_PATH + "/profile";

        var response = userService.updateProfile(keycloakId, request);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Profile updated successfully",
                response, serviceName, path
            )
        );
    }

    @PostMapping("/profile/photo")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> uploadPhoto(
        @RequestHeader(X_USER_ID) String keycloakId,
        @RequestParam("file") MultipartFile file
    ) {

        String path = USER_SERVICE_API_PATH + "/profile/photo";

        var response = userService.uploadProfilePhoto(keycloakId, file);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Profile photo uploaded successfully",
                response, serviceName, path
            )
        );
    }

    @DeleteMapping("/profile/photo")
    public ResponseEntity<ResponseWrapper<Void>> deletePhoto(
        @RequestHeader(X_USER_ID) String keycloakId
    ) {
        String path = USER_SERVICE_API_PATH + "/profile/photo";

        userService.deleteProfilePhoto(keycloakId);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Profile photo removed successfully",
                serviceName, path
            )
        );
    }

    // Internal use — called by other services via Feign client
    // Protected but not role-restricted (any valid JWT)
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> getUserById(
        @PathVariable String userId
    ) {

        String path = USER_SERVICE_API_PATH + "/" + userId;

        var response = userService.getUserById(userId);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "User fetched successfully",
                response, serviceName, path
            )
        );
    }

    @PutMapping("/password")
    public ResponseEntity<ResponseWrapper<Void>> changePassword(
        @RequestHeader(X_USER_ID) String keycloakId,
        @Valid @RequestBody ChangePasswordRequest request
    ) {

        String path = USER_SERVICE_API_PATH + "/password";

        userService.changePassword(keycloakId, request);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Password changed successfully",
                serviceName, path
            )
        );
    }

    @DeleteMapping("/account")
    public ResponseEntity<ResponseWrapper<Void>> deactivateAccount(
        @RequestHeader(X_USER_ID) String keycloakId) {

        String path = USER_SERVICE_API_PATH + "/account";

        userService.deactivateAccount(keycloakId);

        return ResponseEntity.ok(
            ResponseWrapper.ok(
                "Account deactivated",
                serviceName, path
            )
        );
    }


}

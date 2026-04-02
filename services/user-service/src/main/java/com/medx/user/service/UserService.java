package com.medx.user.service;

import com.medx.user.payload.request.ChangePasswordRequest;
import com.medx.user.payload.request.RegisterRequest;
import com.medx.user.payload.request.UpdateProfileRequest;
import com.medx.user.payload.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    public UserProfileResponse register(RegisterRequest request);

    public UserProfileResponse getProfile(String keycloakId);

    public UserProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request);

    public UserProfileResponse uploadProfilePhoto(String keycloakId, MultipartFile file);

    public void deleteProfilePhoto(String keycloakId);

    public void changePassword(String keycloakId, ChangePasswordRequest request);

    public void deactivateAccount(String keycloakId);

    /**
     * This is called by others services as per there requirement via Feign client
     * to get patient details.
     */
    public UserProfileResponse getUserById(String userId);

}

package com.medx.user.service.impl;

import com.medx.user.entity.Address;
import com.medx.user.entity.User;
import com.medx.user.entity.UserProfile;
import com.medx.user.enums.UserStatus;
import com.medx.user.exception.InvalidPasswordException;
import com.medx.user.exception.UserAlreadyExistsException;
import com.medx.user.exception.UserNotFoundExistsException;
import com.medx.user.payload.request.AddressRequest;
import com.medx.user.payload.request.ChangePasswordRequest;
import com.medx.user.payload.request.RegisterRequest;
import com.medx.user.payload.request.UpdateProfileRequest;
import com.medx.user.payload.response.UserProfileResponse;
import com.medx.user.repository.UserProfileRepository;
import com.medx.user.repository.UserRepository;
import com.medx.user.service.KeycloakAdminService;
import com.medx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

import static com.medx.user.mapper.UserProfileMapper.mapToUserProfileResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminService keycloakAdminService;
    // TODO: Cloudinary service need to implement

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.email());

        // Check whether any account with the same email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                "Account with email: " + request.email() + " already exists"
            );
        }
        // Create user in keycloak and get the keycloakId back
        String keycloakId = keycloakAdminService.createUser(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.password()
        );

        try {
            // Save User in database
            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setEmail(request.email());
            user.setPhone(request.phone());
            user.setStatus(UserStatus.ACTIVE);

            user = userRepository.save(user);

            log.debug("User saved successfully with id: {} and keycloakId: {}", user.getId(), user.getKeycloakId());

            // Save UserProfile in database
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setFirstName(request.firstName());
            profile.setLastName(request.lastName());
            profile.setDateOfBirth(request.dateOfBirth());
            profile.setGender(request.gender());

            userProfileRepository.save(profile);
            log.debug("UserProfile saved successfully with id: {}", profile.getUserId());

            return mapToUserProfileResponse(user, profile);

        } catch (Exception e) {
            /*
             * DB save failed after Keycloak user was created. Compensate by disabling Keycloak user
             * to prevent orphaned auth accounts.
             */
            log.error("DB save failed after Keycloak creation, disabling Keycloak user: {}", keycloakId, e);

            keycloakAdminService.disableUser(keycloakId);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        UserProfile profile = findUserProfileByUserId(user.getId());

        return mapToUserProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = findUserByKeycloakId(keycloakId);
        UserProfile profile = findUserProfileByUserId(user.getId());

        // Partial update - only update fields that are present in the request
        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.dateOfBirth() != null) profile.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) profile.setGender(request.gender());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) profile.setAddress(updateAddress(request.address()));

        userRepository.save(user);
        userProfileRepository.save(profile);

        return mapToUserProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadProfilePhoto(String keycloakId, MultipartFile file) {
        // TODO: Implement later
        return null;
    }

    @Override
    @Transactional
    public void deleteProfilePhoto(String keycloakId) {
        // TODO: Implement later
    }

    @Override
    @Transactional
    public void changePassword(String keycloakId, ChangePasswordRequest request) {
        User user = findUserByKeycloakId(keycloakId);

        // verify current password is correct via keycloak
        boolean isCurrentPasswordValid = keycloakAdminService.verifyCurrentPassword(
            user.getEmail(),
            request.currentPassword()
        );
        if (!isCurrentPasswordValid) {
            throw new InvalidPasswordException(
                "You current password is Invalid, Please enter the correct password!"
            );
        }

        // Update password in keycloak
        keycloakAdminService.updatePassword(user.getKeycloakId(), request.newPassword());
    }

    @Override
    @Transactional
    public void deactivateAccount(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);

        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);

        // Disable in keycloak - user can't log in anymore
        keycloakAdminService.disableUser(user.getKeycloakId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(String keycloakId) {
        return getProfile(keycloakId);
    }

    // Helpers

    private Address updateAddress(AddressRequest request) {
        Address address = new Address();

        if (request.street() != null) address.setStreet(request.street());
        if (request.city() != null) address.setCity(request.city());
        if (request.state() != null) address.setState(request.state());
        if (request.country() != null) address.setCountry(request.country());
        if (request.pinCode() != null) address.setPinCode(request.pinCode());

        return address;
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundExistsException(
                "User not found with keycloakId: " + keycloakId
            ));
    }

    private UserProfile findUserProfileByUserId(String userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new UserNotFoundExistsException(
                "Profile not found for user with id: " + userId
            ));
    }
}

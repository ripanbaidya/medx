package com.medx.user.mapper;

import com.medx.user.entity.User;
import com.medx.user.entity.UserProfile;
import com.medx.user.payload.response.AddressResponse;
import com.medx.user.payload.response.UserProfileResponse;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfileResponse mapToUserProfileResponse(
        User user, UserProfile profile
    ) {
        AddressResponse address = null;
        if (profile.getAddress() != null) {
            address = AddressResponse.builder()
                .street(profile.getAddress().getStreet())
                .city(profile.getAddress().getCity())
                .state(profile.getAddress().getState())
                .country(profile.getAddress().getCountry())
                .pinCode(profile.getAddress().getPinCode())
                .build();
        }

        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .phone(user.getPhone())
            .firstName(profile.getFirstName())
            .lastName(profile.getLastName())
            .dateOfBirth(profile.getDateOfBirth())
            .gender(profile.getGender())
            .profilePhotoUrl(profile.getProfilePhotoUrl())
            .status(user.getStatus())
            .address(address)
            .createdAt(user.getCreatedAt())
            .build();
    }
}

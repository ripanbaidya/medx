package com.medx.user.payload.response;

import com.medx.user.enums.Gender;
import com.medx.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Schema(
    name = "UserProfileResponse",
    description = "Response payload for user profile"
)
@Builder
public record UserProfileResponse(
    String id,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Gender gender,
    String profilePhotoUrl,
    UserStatus status,
    AddressResponse address,
    Instant createdAt
) {
}

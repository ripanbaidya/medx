package com.medx.user.payload.request;

import com.medx.user.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Schema(
    name = "UpdateProfileRequest",
    description = "Update request payload for user profile"
)
public record UpdateProfileRequest(

    String firstName,
    String lastName,

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Password must be valid 10-dight Indian mobile number"
    )
    String phone,

    LocalDate dateOfBirth,

    Gender gender,

    @Valid
    AddressRequest address

) {
}

package com.medx.user.payload.request;

import com.medx.user.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(
    name = "RegisterRequest",
    description = "Request payload for user registration"
)
public record RegisterRequest(

    @NotBlank(message = "First name cannot be blank!")
    @Size(
        min = 2,
        max = 50,
        message = "First name must be between 2 and 50 characters"
    )
    String firstName,

    @NotBlank(message = "Last name cannot be blank!")
    @Size(
        min = 2,
        max = 50,
        message = "Last name must be between 2 and 50 characters"
    )
    String lastName,

    @NotBlank(message = "Email is required!")
    @Email(message = "Email must be valid!")
    String email,

    @NotBlank(message = "Password is required!")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
        message = "Password must contain letters and numbers"
    )
    String password,

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone number must be 10 digits Indian mobile number"
    )
    String phone,

    @NotNull(message = "Date of birth is required!")
    LocalDate dateOfBirth,

    @NotNull(message = "Gender is required!")
    @Schema(
        allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"}
    )
    Gender gender
) {
}

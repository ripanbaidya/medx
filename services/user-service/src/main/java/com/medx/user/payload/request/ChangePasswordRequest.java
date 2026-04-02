package com.medx.user.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
    name = "ChangePasswordRequest",
    description = "Request payload for changing password"
)
public record ChangePasswordRequest(

    @NotBlank(message = "Current password is required!")
    String currentPassword,

    @NotBlank(message = "Password is required!")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
        message = "Password must contain letters and numbers"
    )
    String newPassword
) {
}

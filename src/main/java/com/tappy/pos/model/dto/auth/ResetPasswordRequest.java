package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/password-reset/reset */
@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "resetToken is required")
    private String resetToken;

    @NotBlank(message = "newPassword is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String newPassword;
}

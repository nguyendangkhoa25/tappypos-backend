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
    // Max 72 — BCrypt only hashes the first 72 bytes and Spring Security 7 rejects anything longer.
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String newPassword;
}

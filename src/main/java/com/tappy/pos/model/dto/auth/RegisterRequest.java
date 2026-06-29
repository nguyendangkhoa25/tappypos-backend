package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Body for POST /auth/register — self-registration after phone verification.
 * The {@code verificationToken} comes from the register OTP flow
 * ({@code /auth/register/send-otp} → {@code /auth/register/verify-otp}).
 * Any extra client fields (e.g. {@code refreshInBody}) are ignored.
 */
@Getter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "verificationToken is required")
    private String verificationToken;
}

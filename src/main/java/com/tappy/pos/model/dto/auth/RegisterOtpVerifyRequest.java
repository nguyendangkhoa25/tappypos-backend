package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/register/verify-otp */
@Getter
@NoArgsConstructor
public class RegisterOtpVerifyRequest {

    @NotBlank(message = "verificationId is required")
    private String verificationId;

    @NotBlank(message = "code is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String code;
}

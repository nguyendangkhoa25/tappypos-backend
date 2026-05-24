package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/password-reset/verify */
@Getter
@NoArgsConstructor
public class OtpVerifyRequest {

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "otp is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;
}

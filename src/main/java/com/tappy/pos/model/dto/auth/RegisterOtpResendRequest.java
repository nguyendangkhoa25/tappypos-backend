package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/register/resend-otp */
@Getter
@NoArgsConstructor
public class RegisterOtpResendRequest {

    @NotBlank(message = "verificationId is required")
    private String verificationId;
}

package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/register/send-otp */
@Getter
@NoArgsConstructor
public class RegisterOtpRequest {

    @NotBlank(message = "phone is required")
    private String phone;
}

package com.tappy.pos.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Body for POST /auth/password-reset/request */
@Getter
@NoArgsConstructor
public class OtpRequestBody {

    @NotBlank(message = "phone is required")
    private String phone;
}

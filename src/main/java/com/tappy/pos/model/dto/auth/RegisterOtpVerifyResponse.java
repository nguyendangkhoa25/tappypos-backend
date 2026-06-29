package com.tappy.pos.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Returned by POST /auth/register/verify-otp */
@Getter
@AllArgsConstructor
public class RegisterOtpVerifyResponse {
    /** Single-use token (15 min) the client passes to /auth/register to prove phone ownership. */
    private final String verificationToken;
}

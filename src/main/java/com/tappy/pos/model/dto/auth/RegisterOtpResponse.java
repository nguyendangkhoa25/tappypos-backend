package com.tappy.pos.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Returned by POST /auth/register/send-otp and /auth/register/resend-otp */
@Getter
@AllArgsConstructor
public class RegisterOtpResponse {
    /** Opaque handle the client passes back on verify / resend. */
    private final String verificationId;
    /** Phone with middle digits masked, e.g. "84912***456". */
    private final String maskedPhone;
    /** Seconds until the OTP expires (UI countdown). */
    private final long expiresInSeconds;
    /** Seconds until the client may request a resend (UI cooldown). */
    private final long resendAvailableInSeconds;
}

package com.tappy.pos.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Returned by POST /auth/password-reset/verify */
@Getter
@AllArgsConstructor
public class OtpVerifyResponse {
    /**
     * Short-lived UUID reset token (valid for 10 minutes, single-use).
     * Must be passed as-is to POST /auth/password-reset/reset.
     * Never persist this on the client beyond the reset flow.
     */
    private final String resetToken;
}

package com.tappy.pos.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Returned by POST /auth/password-reset/request */
@Getter
@AllArgsConstructor
public class OtpRequestResponse {
    /** Phone with middle digits masked, e.g. "84912***456" */
    private final String maskedPhone;
}

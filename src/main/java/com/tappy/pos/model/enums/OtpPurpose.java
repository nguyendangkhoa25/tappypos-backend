package com.tappy.pos.model.enums;

/**
 * What a phone-verification OTP authorises. Stored on {@code phone_verifications.purpose}.
 *
 * <p>Kept as an enum (rather than a free string) so the verification token issued at verify
 * time is scoped to a single purpose and cannot be replayed across flows.
 */
public enum OtpPurpose {
    /** Self-registration — prove phone ownership before the user account is created. */
    REGISTRATION
}

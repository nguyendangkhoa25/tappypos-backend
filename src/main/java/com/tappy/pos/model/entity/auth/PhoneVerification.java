package com.tappy.pos.model.entity.auth;

import com.tappy.pos.model.entity.BaseEntity;
import com.tappy.pos.model.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A phone-ownership OTP issued before an account exists (self-registration).
 * Master-level table (no tenant_id) — see {@code V010__phone_verifications.sql}.
 *
 * <p>Distinct from {@link PasswordResetOtp}, which is keyed by {@code user_id} (the user already
 * exists for a password reset). Here the user does not exist yet, so the row is keyed only by the
 * phone + an opaque {@link #verificationId} handle.
 *
 * Lifecycle: PENDING → VERIFIED → USED  |  PENDING → LOCKED (3 wrong guesses)
 */
@Entity
@Table(name = "phone_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhoneVerification extends BaseEntity {

    /** Opaque UUID handle returned to the client and passed back on verify / resend. */
    @Column(name = "verification_id", nullable = false, length = 36, unique = true)
    private String verificationId;

    /** Normalised phone number (84XXXXXXXXX send form). */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose = OtpPurpose.REGISTRATION;

    /** SHA-256(otp || salt) */
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "otp_salt", nullable = false, length = 32)
    private String otpSalt;

    /** SHA-256(UUID verification token) — set when OTP is verified. */
    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "wrong_attempts", nullable = false)
    private Integer wrongAttempts = 0;

    @Builder.Default
    @Column(name = "resend_count", nullable = false)
    private Integer resendCount = 0;

    @Column(name = "last_resend_at")
    private LocalDateTime lastResendAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** OTP expires 5 minutes after issue. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Verification token expires 15 minutes after the OTP is verified. */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}

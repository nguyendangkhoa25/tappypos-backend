package com.tappy.pos.model.entity.auth;

import com.tappy.pos.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Tracks every forgot-password OTP issued.
 * Master-level table (no tenant_id) — issued before the user is authenticated.
 *
 * Lifecycle: PENDING → VERIFIED → USED
 *            PENDING → LOCKED   (3 wrong guesses)
 *            PENDING → EXPIRED  (batch cleanup; expires_at < NOW())
 */
@Entity
@Table(name = "password_reset_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PasswordResetOtp extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Normalised phone number (84XXXXXXXXX format) */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** SHA-256(otp || salt) */
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "otp_salt", nullable = false, length = 32)
    private String otpSalt;

    /** SHA-256(UUID reset token) — set when OTP is verified */
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

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

    /** Zalo ZNS message_id for delivery tracking */
    @Column(name = "zns_message_id", length = 100)
    private String znsMessageId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** OTP expires 5 minutes after creation */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Reset token expires 10 minutes after OTP verification */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}

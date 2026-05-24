package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    /**
     * Most recent PENDING, non-expired OTP row for this phone.
     * Used during verify step.
     */
    @Query(value = """
            SELECT * FROM password_reset_otps
            WHERE phone = :phone
              AND status = 'PENDING'
              AND expires_at > NOW()
              AND deleted = false
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<PasswordResetOtp> findLatestPendingByPhone(@Param("phone") String phone);

    /**
     * Find a VERIFIED row by its reset-token hash with a valid (non-expired) token.
     * Used during reset-password step.
     */
    @Query(value = """
            SELECT * FROM password_reset_otps
            WHERE reset_token_hash = :tokenHash
              AND status = 'VERIFIED'
              AND token_expires_at > NOW()
              AND deleted = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<PasswordResetOtp> findByResetTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Count how many OTP requests were made for this phone in the given window.
     * Used to enforce the resend rate limit (3 per 10 minutes).
     */
    @Query(value = """
            SELECT COUNT(*) FROM password_reset_otps
            WHERE phone = :phone
              AND created_at > :since
              AND deleted = false
            """, nativeQuery = true)
    int countRecentRequestsByPhone(@Param("phone") String phone,
                                   @Param("since") LocalDateTime since);
}

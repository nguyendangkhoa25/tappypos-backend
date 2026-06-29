package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /** Lookup by the opaque client handle (verify / resend steps). */
    @Query(value = """
            SELECT * FROM phone_verifications
            WHERE verification_id = :verificationId
              AND deleted = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<PhoneVerification> findByVerificationId(@Param("verificationId") String verificationId);

    /**
     * Find a VERIFIED row by its verification-token hash with a still-valid token.
     * Used at register time to consume the token.
     */
    @Query(value = """
            SELECT * FROM phone_verifications
            WHERE verification_token_hash = :tokenHash
              AND status = 'VERIFIED'
              AND token_expires_at > NOW()
              AND deleted = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<PhoneVerification> findByVerificationTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Count how many OTPs were issued for this phone + purpose in the given window.
     * Used to enforce the resend rate limit.
     */
    @Query(value = """
            SELECT COUNT(*) FROM phone_verifications
            WHERE phone = :phone
              AND purpose = :purpose
              AND created_at > :since
              AND deleted = false
            """, nativeQuery = true)
    int countRecentRequests(@Param("phone") String phone,
                            @Param("purpose") String purpose,
                            @Param("since") LocalDateTime since);

    /**
     * Atomically consume the verification token: flip VERIFIED → USED in a single statement, so two
     * concurrent registrations with the same token cannot both succeed. Returns the number of rows
     * updated (1 = this caller won the race, 0 = already used / expired).
     */
    @Modifying
    @Query(value = """
            UPDATE phone_verifications
            SET status = 'USED', used_at = NOW(), updated_at = NOW()
            WHERE verification_token_hash = :tokenHash
              AND status = 'VERIFIED'
              AND token_expires_at > NOW()
              AND deleted = false
            """, nativeQuery = true)
    int markTokenUsed(@Param("tokenHash") String tokenHash);
}

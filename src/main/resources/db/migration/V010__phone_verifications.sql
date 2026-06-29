-- ============================================================================
-- V010: Phone verification OTP for self-registration
-- ============================================================================
-- A generic, user-agnostic OTP table used to prove phone ownership BEFORE an
-- account exists. The existing `password_reset_otps` table cannot be reused for
-- registration because it has `user_id NOT NULL REFERENCES users(id)` — at
-- registration time no user row exists yet.
--
-- Master-level table (no tenant_id, no RLS) — issued pre-auth, exactly like
-- `password_reset_otps`. The `purpose` column keeps it reusable for future
-- flows (e.g. CHANGE_PHONE) without another table.
--
-- Flow:
--   send-otp   → create row (PENDING), deliver OTP via Tappy Message (Zalo)
--   verify-otp → validate code, issue a single-use verification_token (VERIFIED)
--   register   → consume verification_token, create the user (USED)
--
-- Lifecycle: PENDING → VERIFIED → USED  |  or LOCKED (3 wrong guesses)
-- ============================================================================

CREATE TABLE IF NOT EXISTS phone_verifications (
    id                      BIGSERIAL    PRIMARY KEY,
    -- Opaque UUID handle returned to the client (never the sequential id, to
    -- avoid enumeration). Passed back on verify / resend.
    verification_id         VARCHAR(36)  NOT NULL UNIQUE,
    -- Normalised phone number (84XXXXXXXXX send form)
    phone                   VARCHAR(20)  NOT NULL,
    -- What this OTP authorises: REGISTRATION (extensible)
    purpose                 VARCHAR(30)  NOT NULL DEFAULT 'REGISTRATION',
    -- OTP credential (SHA-256 of otp || salt — matches password_reset_otps)
    otp_hash                VARCHAR(64)  NOT NULL,
    otp_salt                VARCHAR(32)  NOT NULL,
    -- Single-use verification token issued after the OTP is verified (SHA-256 of UUID)
    verification_token_hash VARCHAR(64)  DEFAULT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    wrong_attempts          INT          NOT NULL DEFAULT 0,
    resend_count            INT          NOT NULL DEFAULT 0,
    last_resend_at          TIMESTAMP    DEFAULT NULL,
    ip_address              VARCHAR(45)  DEFAULT NULL,
    expires_at              TIMESTAMP    NOT NULL,       -- OTP validity (NOW + 5 min)
    token_expires_at        TIMESTAMP    DEFAULT NULL,   -- verification token validity (NOW + 15 min)
    verified_at             TIMESTAMP    DEFAULT NULL,
    used_at                 TIMESTAMP    DEFAULT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP    DEFAULT NULL
);

-- Lookup by the opaque handle (verify / resend) is already served by the UNIQUE
-- constraint's index on verification_id — no separate index needed.
-- Rate-limit / latest-pending lookups
CREATE INDEX IF NOT EXISTS idx_phone_verif_phone_purpose
    ON phone_verifications (phone, purpose, status, expires_at);
-- Consume the verification token at register time
CREATE INDEX IF NOT EXISTS idx_phone_verif_token
    ON phone_verifications (verification_token_hash) WHERE verification_token_hash IS NOT NULL;

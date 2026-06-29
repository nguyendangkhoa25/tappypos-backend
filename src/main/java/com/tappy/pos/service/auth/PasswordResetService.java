package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.entity.auth.PasswordResetOtp;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.PasswordResetOtpRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.messaging.TappyMessageClient;
import com.tappy.pos.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Forgot-password flow via the Tappy Message OTP service.
 *
 * Three steps:
 *  1. requestOtp(phone)          — lookup user, issue OTP, send via the message service
 *  2. verifyOtp(phone, otp)      — validate OTP, return short-lived resetToken
 *  3. resetPassword(token, pwd)  — validate token, hash new password, mark used
 *
 * Security invariants:
 *  - Step 1 always returns HTTP 200 (never reveals if phone exists).
 *  - OTP hash stored as SHA-256(otp || salt); token stored as SHA-256(UUID).
 *  - 3 wrong OTP guesses → LOCKED for 15 minutes.
 *  - 3 OTP requests per 10 minutes → 429.
 *  - Reset token valid for 10 minutes and single-use.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PasswordResetService {

    static final int OTP_TTL_MINUTES          = 5;
    static final int RESET_TOKEN_TTL_MINUTES  = 10;
    static final int MAX_WRONG_ATTEMPTS       = 3;
    static final int LOCK_EXTRA_MINUTES       = 15;   // added on top of OTP_TTL
    static final int MAX_RESENDS_PER_WINDOW   = 3;
    static final int RESEND_WINDOW_MINUTES    = 10;

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository             userRepository;
    private final PasswordEncoder            passwordEncoder;
    private final TappyMessageClient         tappyMessageClient;
    private final MessageService             messageService;

    // ── Step 1 ────────────────────────────────────────────────────────────────

    /**
     * Request a password-reset OTP for the given phone number.
     *
     * <p><b>Anti-enumeration: always returns the masked phone with an identical HTTP 200</b>,
     * regardless of whether the phone is unregistered, rate-limited, or undeliverable. A caller
     * can never tell from the response (status or body) whether an account exists for that number.
     * This mirrors the tappy/build forgot-password behaviour.
     *
     * <p>The OTP row and the Zalo send only happen for a registered phone that is within the rate
     * limit. Every other path — unknown phone, rate-limit exceeded, "not on Zalo", transient send
     * failure — is logged and swallowed so the externally observable result is the same.
     */
    @Transactional
    public String requestOtp(String rawPhone, String ipAddress) {
        String phone = PhoneUtil.normalizePhone(rawPhone);
        String masked = PhoneUtil.maskPhone(phone);

        // Rate-limit: ≤ MAX_RESENDS_PER_WINDOW requests in the past RESEND_WINDOW_MINUTES.
        // Exceeding it stops silently (no 429): only a registered phone can ever accumulate OTP
        // rows, so surfacing the limit would leak that the phone is registered. The client enforces
        // the resend cooldown in the UI; the server still caps actual sends here.
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RESEND_WINDOW_MINUTES);
        int recentCount = otpRepository.countRecentRequestsByPhone(phone, windowStart);
        if (recentCount >= MAX_RESENDS_PER_WINDOW) {
            log.warn("[OTP] Rate limit hit for phone={} — silently ignored", masked);
            return masked;
        }

        // Silent exit if no user found — same response so callers can't enumerate phones.
        // Users are stored in the local "0..." form (registration keeps the number as typed),
        // so look up by the local form first, then fall back to the "84..." form for any
        // legacy rows that may have been persisted normalized.
        String localPhone = PhoneUtil.localizePhone(phone);
        var userOpt = userRepository.findByPhoneGlobal(localPhone);
        if (userOpt.isEmpty() && !localPhone.equals(phone)) {
            userOpt = userRepository.findByPhoneGlobal(phone);
        }
        if (userOpt.isEmpty()) {
            log.info("[OTP] Reset requested for unregistered phone={}", masked);
            return masked;
        }
        User user = userOpt.get();

        String otp  = generateOtp();
        String salt = generateSalt();

        PasswordResetOtp record = PasswordResetOtp.builder()
                .userId(user.getId())
                .phone(phone)
                .otpHash(sha256(otp + salt))
                .otpSalt(salt)
                .status("PENDING")
                .wrongAttempts(0)
                .resendCount(recentCount)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .build();
        otpRepository.save(record);

        // Best-effort delivery: never surface a delivery failure to the caller. A failed send (bad
        // recipient, provider rejection, transient error) can only occur for a REGISTERED phone, so
        // propagating it would leak account existence. The client never throws; we inspect its Result
        // only to log. The user can simply request a new code.
        TappyMessageClient.Result result = tappyMessageClient.sendOtp(phone, otp, record.getId());
        if (result.sent()) {
            log.info("[OTP] Issued for user={} phone={} rowId={}",
                    user.getUsername(), masked, record.getId());
        } else {
            log.warn("[OTP] Delivery failed (best-effort) for phone={} rowId={}: errorCode={}",
                    masked, record.getId(), result.errorCode());
        }
        return masked;
    }

    // ── Step 2 ────────────────────────────────────────────────────────────────

    /**
     * Verify the 6-digit OTP entered by the user.
     *
     * @return a short-lived reset token (UUID, valid for {@value #RESET_TOKEN_TTL_MINUTES} minutes)
     * @throws BadRequestException       on invalid / expired OTP or wrong guess
     * @throws ResponseStatusException   429 when OTP is LOCKED
     */
    public String verifyOtp(String rawPhone, String submittedOtp) {
        String phone  = PhoneUtil.normalizePhone(rawPhone);
        String masked = PhoneUtil.maskPhone(phone);

        PasswordResetOtp record = otpRepository.findLatestPendingByPhone(phone)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.otp.invalid.or.expired")));

        // Check locked status (may already be LOCKED from a previous transaction)
        if (record.getWrongAttempts() >= MAX_WRONG_ATTEMPTS) {
            log.warn("[OTP] Verify attempted on locked OTP rowId={} phone={}", record.getId(), masked);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    messageService.getMessage("error.otp.locked"));
        }

        // Validate hash
        String expectedHash = sha256(submittedOtp + record.getOtpSalt());
        if (!expectedHash.equals(record.getOtpHash())) {
            int newAttempts = record.getWrongAttempts() + 1;
            record.setWrongAttempts(newAttempts);
            if (newAttempts >= MAX_WRONG_ATTEMPTS) {
                record.setStatus("LOCKED");
                log.warn("[OTP] Locked after {} wrong attempts rowId={} phone={}",
                        MAX_WRONG_ATTEMPTS, record.getId(), masked);
            }
            otpRepository.save(record);
            int remaining = MAX_WRONG_ATTEMPTS - newAttempts;
            if (remaining <= 0) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        messageService.getMessage("error.otp.locked"));
            }
            throw new BadRequestException(
                    messageService.getMessage("error.otp.wrong.remaining", remaining));
        }

        // OTP correct — generate single-use reset token
        String resetToken = UUID.randomUUID().toString();
        record.setStatus("VERIFIED");
        record.setResetTokenHash(sha256(resetToken));
        record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        otpRepository.save(record);

        log.info("[OTP] Verified rowId={} phone={}", record.getId(), masked);
        return resetToken;
    }

    // ── Step 3 ────────────────────────────────────────────────────────────────

    /**
     * Set a new password using the reset token from step 2.
     * Also unlocks the account and clears failed-login counter.
     *
     * @throws BadRequestException on expired/invalid token or weak password
     */
    public void resetPassword(String resetToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException(
                    messageService.getMessage("error.otp.password.min.length"));
        }

        String tokenHash = sha256(resetToken);
        PasswordResetOtp record = otpRepository.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.otp.reset.token.invalid")));

        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.user.not.found", record.getUserId())));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        record.setStatus("USED");
        record.setUsedAt(LocalDateTime.now());
        otpRepository.save(record);

        log.info("[OTP] Password reset completed for user={} rowId={}",
                user.getUsername(), record.getId());
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private String generateOtp() {
        // Pad to 6 digits, e.g. 007 becomes "000007"
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}

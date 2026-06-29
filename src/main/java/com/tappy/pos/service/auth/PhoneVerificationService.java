package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.DuplicateResourceException;
import com.tappy.pos.model.dto.auth.RegisterOtpResponse;
import com.tappy.pos.model.entity.auth.PhoneVerification;
import com.tappy.pos.model.enums.OtpPurpose;
import com.tappy.pos.repository.auth.PhoneVerificationRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.messaging.TappyMessageClient;
import com.tappy.pos.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Phone-ownership OTP for self-registration via the Tappy Message OTP service (Zalo).
 *
 * <p>Four steps:
 * <ol>
 *   <li>{@link #sendOtp}   — verify the phone is free, issue an OTP, deliver it via Zalo.</li>
 *   <li>{@link #resendOtp} — reissue an OTP on the same verification (subject to a cooldown).</li>
 *   <li>{@link #verifyOtp} — validate the code, return a single-use verification token.</li>
 *   <li>{@link #consumeVerificationToken} — called by registration to prove phone ownership.</li>
 * </ol>
 *
 * <p>Unlike {@link PasswordResetService} this flow is <b>not</b> anti-enumeration: registration must
 * tell the caller when a phone is already taken (a 409), so the UI can route them to login. The OTP
 * itself is hashed exactly the same way (SHA-256(otp || salt)); 3 wrong guesses LOCK the row.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PhoneVerificationService {

    static final int OTP_TTL_MINUTES                = 5;
    static final int VERIFICATION_TOKEN_TTL_MINUTES = 15;
    static final int MAX_WRONG_ATTEMPTS             = 3;
    static final int RESEND_COOLDOWN_SECONDS        = 60;
    static final int MAX_RESENDS_PER_WINDOW         = 5;
    static final int RESEND_WINDOW_MINUTES          = 10;

    private final PhoneVerificationRepository verificationRepository;
    private final UserRepository              userRepository;
    private final PasswordEncoder             passwordEncoder; // reserved; kept for symmetry with PasswordResetService
    private final TappyMessageClient          tappyMessageClient;
    private final MessageService              messageService;

    /**
     * DEV/TEST ONLY — a fixed OTP that makes E2E deterministic. Set in the {@code dev} profile
     * (application-dev.properties), blank in prod so codes stay random. Mirrors tappy/land's
     * {@code tappy.otp.test-code}.
     */
    @Value("${tappy.otp.test-code:}")
    private String testOtpCode;

    // ── Step 1: send ────────────────────────────────────────────────────────────

    /**
     * Issue a registration OTP for the given phone.
     *
     * @throws DuplicateResourceException (409) if the phone is already registered
     * @throws ResponseStatusException    (429) if too many OTPs were requested for this phone recently
     */
    public RegisterOtpResponse sendOtp(String rawPhone, String ipAddress) {
        String phone  = PhoneUtil.normalizePhone(rawPhone);
        String masked = PhoneUtil.maskPhone(phone);

        if (isPhoneRegistered(phone)) {
            throw new DuplicateResourceException(messageService.getMessage("error.auth.phone.registered"));
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RESEND_WINDOW_MINUTES);
        int recentCount = verificationRepository.countRecentRequests(
                phone, OtpPurpose.REGISTRATION.name(), windowStart);
        if (recentCount >= MAX_RESENDS_PER_WINDOW) {
            log.warn("[REG-OTP] Rate limit hit for phone={} — rejected", masked);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    messageService.getMessage("error.otp.resend.too.soon"));
        }

        String otp  = generateOtp();
        String salt = generateSalt();

        PhoneVerification record = PhoneVerification.builder()
                .verificationId(UUID.randomUUID().toString())
                .phone(phone)
                .purpose(OtpPurpose.REGISTRATION)
                .otpHash(sha256(otp + salt))
                .otpSalt(salt)
                .status("PENDING")
                .wrongAttempts(0)
                .resendCount(recentCount)
                .lastResendAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .build();
        verificationRepository.save(record);

        deliver(record, otp, masked);
        return toResponse(record);
    }

    // ── Step 2: resend ────────────────────────────────────────────────────────────

    /**
     * Reissue an OTP on an existing verification.
     *
     * @throws BadRequestException     if the verification is unknown or no longer pending
     * @throws ResponseStatusException (429) if the resend cooldown has not elapsed
     */
    public RegisterOtpResponse resendOtp(String verificationId, String ipAddress) {
        PhoneVerification record = verificationRepository.findByVerificationId(verificationId)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.otp.invalid.or.expired")));

        if (!"PENDING".equals(record.getStatus())) {
            throw new BadRequestException(messageService.getMessage("error.otp.invalid.or.expired"));
        }

        String masked = PhoneUtil.maskPhone(record.getPhone());

        // Resend cooldown — keyed to the last send, survives restarts (DB-backed).
        if (record.getLastResendAt() != null
                && record.getLastResendAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            log.warn("[REG-OTP] Resend too soon for phone={} vid={}", masked, verificationId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    messageService.getMessage("error.otp.resend.too.soon"));
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RESEND_WINDOW_MINUTES);
        int recentCount = verificationRepository.countRecentRequests(
                record.getPhone(), OtpPurpose.REGISTRATION.name(), windowStart);
        if (recentCount >= MAX_RESENDS_PER_WINDOW) {
            log.warn("[REG-OTP] Resend rate limit hit for phone={} vid={}", masked, verificationId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    messageService.getMessage("error.otp.resend.too.soon"));
        }

        String otp  = generateOtp();
        String salt = generateSalt();
        record.setOtpHash(sha256(otp + salt));
        record.setOtpSalt(salt);
        record.setWrongAttempts(0);
        record.setResendCount(record.getResendCount() + 1);
        record.setLastResendAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        verificationRepository.save(record);

        deliver(record, otp, masked);
        return toResponse(record);
    }

    // ── Step 3: verify ────────────────────────────────────────────────────────────

    /**
     * Verify the 6-digit OTP.
     *
     * @return a single-use verification token (valid {@value #VERIFICATION_TOKEN_TTL_MINUTES} minutes)
     * @throws BadRequestException     on invalid / expired OTP or wrong guess
     * @throws ResponseStatusException (429) when the OTP is LOCKED
     */
    public String verifyOtp(String verificationId, String submittedCode) {
        PhoneVerification record = verificationRepository.findByVerificationId(verificationId)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.otp.invalid.or.expired")));

        String masked = PhoneUtil.maskPhone(record.getPhone());

        if ("LOCKED".equals(record.getStatus()) || record.getWrongAttempts() >= MAX_WRONG_ATTEMPTS) {
            log.warn("[REG-OTP] Verify on locked vid={} phone={}", verificationId, masked);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    messageService.getMessage("error.otp.locked"));
        }

        if (!"PENDING".equals(record.getStatus())
                || record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(messageService.getMessage("error.otp.invalid.or.expired"));
        }

        String expectedHash = sha256(submittedCode + record.getOtpSalt());
        if (!expectedHash.equals(record.getOtpHash())) {
            int newAttempts = record.getWrongAttempts() + 1;
            record.setWrongAttempts(newAttempts);
            if (newAttempts >= MAX_WRONG_ATTEMPTS) {
                record.setStatus("LOCKED");
                log.warn("[REG-OTP] Locked after {} wrong attempts vid={} phone={}",
                        MAX_WRONG_ATTEMPTS, verificationId, masked);
            }
            verificationRepository.save(record);
            int remaining = MAX_WRONG_ATTEMPTS - newAttempts;
            if (remaining <= 0) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        messageService.getMessage("error.otp.locked"));
            }
            throw new BadRequestException(
                    messageService.getMessage("error.otp.wrong.remaining", remaining));
        }

        // OTP correct — issue a single-use verification token.
        String verificationToken = UUID.randomUUID().toString();
        record.setStatus("VERIFIED");
        record.setVerifiedAt(LocalDateTime.now());
        record.setVerificationTokenHash(sha256(verificationToken));
        record.setTokenExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_TTL_MINUTES));
        verificationRepository.save(record);

        log.info("[REG-OTP] Verified vid={} phone={}", verificationId, masked);
        return verificationToken;
    }

    // ── Step 4: consume (called from registration) ────────────────────────────────

    /**
     * Validate a verification token at register time and mark it used. Proves the caller verified
     * ownership of {@code rawPhone} for {@code purpose}.
     *
     * @throws BadRequestException if the token is missing, expired, already used, for another phone,
     *                             or issued for a different purpose
     */
    public void consumeVerificationToken(String verificationToken, String rawPhone, OtpPurpose purpose) {
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new BadRequestException(messageService.getMessage("error.otp.verification.token.invalid"));
        }
        String phone = PhoneUtil.normalizePhone(rawPhone);

        String tokenHash = sha256(verificationToken);
        PhoneVerification record = verificationRepository
                .findByVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(
                        messageService.getMessage("error.otp.verification.token.invalid")));

        if (record.getPurpose() != purpose || !phone.equals(record.getPhone())) {
            log.warn("[REG-OTP] Token mismatch — purpose/phone do not match vid={}", record.getVerificationId());
            throw new BadRequestException(messageService.getMessage("error.otp.verification.token.invalid"));
        }

        // Atomic single-use transition: only one concurrent caller can flip VERIFIED → USED.
        int updated = verificationRepository.markTokenUsed(tokenHash);
        if (updated == 0) {
            log.warn("[REG-OTP] Token already consumed/expired (race) vid={}", record.getVerificationId());
            throw new BadRequestException(messageService.getMessage("error.otp.verification.token.invalid"));
        }
        log.info("[REG-OTP] Token consumed vid={} phone={}",
                record.getVerificationId(), PhoneUtil.maskPhone(phone));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Users are stored in the local "0..." form (registration keeps the number as typed), so look up
     * by the local form first, then fall back to the "84..." form for any legacy normalized rows.
     */
    private boolean isPhoneRegistered(String normalizedPhone) {
        String localPhone = PhoneUtil.localizePhone(normalizedPhone);
        if (userRepository.findByUsernameGlobal(localPhone).isPresent()) {
            return true;
        }
        return !localPhone.equals(normalizedPhone)
                && userRepository.findByUsernameGlobal(normalizedPhone).isPresent();
    }

    private void deliver(PhoneVerification record, String otp, String masked) {
        // Best-effort: a delivery failure is logged but not surfaced. The phone is already known to be
        // free at this point, so the user can simply request a new code.
        TappyMessageClient.Result result = tappyMessageClient.sendOtp(record.getPhone(), otp, record.getId());
        if (result.sent()) {
            log.info("[REG-OTP] Issued vid={} phone={} rowId={}",
                    record.getVerificationId(), masked, record.getId());
        } else {
            log.warn("[REG-OTP] Delivery failed (best-effort) phone={} rowId={}: errorCode={}",
                    masked, record.getId(), result.errorCode());
        }
    }

    private RegisterOtpResponse toResponse(PhoneVerification record) {
        long expiresIn = Math.max(0,
                Duration.between(LocalDateTime.now(), record.getExpiresAt()).getSeconds());
        return new RegisterOtpResponse(
                record.getVerificationId(),
                PhoneUtil.maskPhone(record.getPhone()),
                expiresIn,
                RESEND_COOLDOWN_SECONDS);
    }

    private String generateOtp() {
        // DEV/TEST ONLY: a fixed code (tappy.otp.test-code) makes E2E deterministic. Blank in prod → random.
        if (testOtpCode != null && !testOtpCode.isBlank()) {
            return testOtpCode;
        }
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

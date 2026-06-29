package com.tappy.pos.integration;

import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.messaging.TappyMessageClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Full-stack happy-path for the forgot-password OTP chain, end-to-end on a real PostgreSQL.
 *
 * <p>Drives the real HTTP API through the full filter chain (JWT auth, {@code TenantInterceptor},
 * feature gating, RLS) and the real {@code PasswordResetService} + JPA persistence:
 * <ol>
 *   <li>register a brand-new user with a known phone,</li>
 *   <li>{@code POST /auth/password-reset/request} → an OTP row is issued and handed to the
 *       {@link TappyMessageClient},</li>
 *   <li>{@code POST /auth/password-reset/verify} with that OTP → a single-use reset token,</li>
 *   <li>{@code POST /auth/password-reset/reset} with the token → the password is actually changed,</li>
 *   <li>prove the change took: login succeeds with the NEW password and fails with the OLD one.</li>
 * </ol>
 *
 * <p><b>How the OTP is obtained without a real Zalo send:</b> the {@code dev} profile leaves
 * {@code tappy.message.enabled=false}, so the real {@link TappyMessageClient#sendOtp} short-circuits
 * (logs, returns {@code ok()}) without any network call. We wrap the real bean in a Mockito
 * {@link MockitoSpyBean} purely to (a) assert {@code PasswordResetService} delegates to it with the
 * normalized phone + a 6-digit code, and (b) capture that generated code so the test can complete the
 * verify→reset chain. The actual HTTP serialization to {@code msg.tappy.vn} is covered by the unit
 * test {@code TappyMessageClientTest} (MockRestServiceServer); this IT proves the DB-backed flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("OTP password-reset — full request→verify→reset chain (full stack on real Postgres)")
class PasswordResetOtpFlowIT extends AbstractApiIT {

    // Real bean (dev: enabled=false → no network); spied to capture the OTP + assert delegation.
    @MockitoSpyBean
    private TappyMessageClient tappyMessageClient;

    @Autowired private UserRepository userRepository;   // cross-tenant lookup to assert the stored hash
    @Autowired private PasswordEncoder passwordEncoder;  // BCrypt — the real encoder the service uses

    @Test
    @Order(1)
    @DisplayName("Register → request OTP → verify → reset changes the password (old fails, new works)")
    void fullResetChangesPassword() {
        String phone   = uniquePhone();
        String oldPass = "register123";
        String newPass = "brand-new-pass-9";

        // 1. Register a tenant-less user with a known phone (registration now requires a verified
        //    phone via the Zalo OTP flow — no X-Tenant-ID on any /auth/register* endpoint).
        registerViaOtp(phone, oldPass);
        User before = userRepository.findByUsernameGlobal(phone).orElseThrow();
        assertThat(passwordEncoder.matches(oldPass, before.getPassword()))
                .as("old password hash stored at registration").isTrue();

        // 2. Request the OTP. Always 200 + masked phone (anti-enumeration). The dev-disabled client
        //    is spied so we can capture the plaintext OTP that PasswordResetService generated.
        ResponseEntity<String> requestOtp = post("/auth/password-reset/request",
                Map.of("phone", phone), jsonHeaders());
        assertThat(requestOtp.getStatusCode().is2xxSuccessful())
                .as("POST /auth/password-reset/request: %s", requestOtp.getBody()).isTrue();
        assertThat(path(requestOtp, "data", "maskedPhone"))
                .as("masked phone in response").isNotBlank();

        ArgumentCaptor<String> phoneArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> otpArg   = ArgumentCaptor.forClass(String.class);
        verify(tappyMessageClient).sendOtp(phoneArg.capture(), otpArg.capture(), anyLong());
        assertThat(phoneArg.getValue()).as("normalized phone passed to message service").matches("84\\d{9}");
        String otp = otpArg.getValue();
        assertThat(otp).as("generated OTP").matches("\\d{6}");

        // 3. Verify the OTP → single-use reset token.
        ResponseEntity<String> verify = post("/auth/password-reset/verify",
                Map.of("phone", phone, "otp", otp), jsonHeaders());
        assertThat(verify.getStatusCode().is2xxSuccessful())
                .as("POST /auth/password-reset/verify: %s", verify.getBody()).isTrue();
        String resetToken = path(verify, "data", "resetToken");
        assertThat(resetToken).as("reset token").isNotBlank();

        // 4. Reset the password with the token.
        ResponseEntity<String> reset = post("/auth/password-reset/reset",
                Map.of("resetToken", resetToken, "newPassword", newPass), jsonHeaders());
        assertThat(reset.getStatusCode().is2xxSuccessful())
                .as("POST /auth/password-reset/reset: %s", reset.getBody()).isTrue();

        // 5. Prove the password actually changed in the DB: the new password now matches the stored
        //    BCrypt hash, the old one no longer does, and the account was unlocked by the reset.
        User after = userRepository.findByUsernameGlobal(phone).orElseThrow();
        assertThat(passwordEncoder.matches(newPass, after.getPassword()))
                .as("new password persisted after reset").isTrue();
        assertThat(passwordEncoder.matches(oldPass, after.getPassword()))
                .as("old password no longer valid after reset").isFalse();
        assertThat(after.getAccountNonLocked())
                .as("account unlocked by reset").isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Reset request for an UNREGISTERED phone returns 200 but sends no OTP (anti-enumeration)")
    void unregisteredPhoneSendsNothing() {
        String phone = uniquePhone();   // never registered

        ResponseEntity<String> requestOtp = post("/auth/password-reset/request",
                Map.of("phone", phone), jsonHeaders());
        assertThat(requestOtp.getStatusCode().is2xxSuccessful())
                .as("POST /auth/password-reset/request (unregistered): %s", requestOtp.getBody()).isTrue();
        assertThat(path(requestOtp, "data", "maskedPhone")).isNotBlank();

        // No user → no OTP row, no delivery: the message client must never be touched.
        verify(tappyMessageClient, never()).sendOtp(any(), any(), anyLong());
    }

    @Test
    @Order(3)
    @DisplayName("Wrong OTP is rejected and never yields a reset token")
    void wrongOtpRejected() {
        String phone   = uniquePhone();
        String oldPass = "register123";

        registerViaOtp(phone, oldPass);

        ResponseEntity<String> requestOtp = post("/auth/password-reset/request",
                Map.of("phone", phone), jsonHeaders());
        assertThat(requestOtp.getStatusCode().is2xxSuccessful()).isTrue();

        // Capture the real OTP, then submit a deliberately different 6-digit code.
        ArgumentCaptor<String> otpArg = ArgumentCaptor.forClass(String.class);
        verify(tappyMessageClient).sendOtp(any(), otpArg.capture(), anyLong());
        String realOtp = otpArg.getValue();
        String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        ResponseEntity<String> verify = post("/auth/password-reset/verify",
                Map.of("phone", phone, "otp", wrongOtp), jsonHeaders());
        assertThat(verify.getStatusCode().is4xxClientError())
                .as("wrong OTP must 4xx: %s", verify.getBody()).isTrue();
        assertThat(path(verify, "data", "resetToken"))
                .as("no reset token on wrong OTP").isNull();
    }

    /**
     * Register a tenant-less user through the Zalo-OTP registration flow
     * (send-otp → capture code via the spied client → verify-otp → register with the token).
     * Clears the spy's recorded invocations afterwards so the caller's own
     * {@code verify(tappyMessageClient).sendOtp(...)} (times-1) assertions on the password-reset
     * send are not polluted by this registration send.
     */
    private void registerViaOtp(String phone, String password) {
        ResponseEntity<String> sendOtp = post("/auth/register/send-otp",
                Map.of("phone", phone), jsonHeaders());
        assertThat(sendOtp.getStatusCode().is2xxSuccessful())
                .as("POST /auth/register/send-otp: %s", sendOtp.getBody()).isTrue();
        String verificationId = path(sendOtp, "data", "verificationId");

        ArgumentCaptor<String> otpArg = ArgumentCaptor.forClass(String.class);
        verify(tappyMessageClient).sendOtp(any(), otpArg.capture(), anyLong());
        String code = otpArg.getValue();

        ResponseEntity<String> verifyOtp = post("/auth/register/verify-otp",
                Map.of("verificationId", verificationId, "code", code), jsonHeaders());
        assertThat(verifyOtp.getStatusCode().is2xxSuccessful())
                .as("POST /auth/register/verify-otp: %s", verifyOtp.getBody()).isTrue();
        String verificationToken = path(verifyOtp, "data", "verificationToken");

        ResponseEntity<String> register = post("/auth/register",
                Map.of("phone", phone, "password", password, "verificationToken", verificationToken),
                jsonHeaders());
        assertThat(register.getStatusCode().is2xxSuccessful())
                .as("POST /auth/register: %s", register.getBody()).isTrue();

        // Reset the spy so the password-reset send is the only sendOtp invocation the caller asserts on.
        clearInvocations(tappyMessageClient);
    }

    private String uniquePhone() {
        return "09" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }
}

package com.tappy.pos.service.auth;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ZaloSendException;
import com.tappy.pos.exception.ZaloUserNotReachableException;
import com.tappy.pos.model.entity.auth.PasswordResetOtp;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.PasswordResetOtpRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock private PasswordResetOtpRepository otpRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ZaloZnsService zaloZnsService;
    @Mock private MessageService messageService;

    @InjectMocks
    private PasswordResetService service;

    private User user;

    @BeforeEach
    void setUp() {
        when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));
        user = new User();
        user.setId(5L);
        user.setUsername("bob");
    }

    // ── requestOtp ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("requestOtp issues OTP and fires ZNS for a registered phone")
    void requestOtp_registered() {
        when(otpRepository.countRecentRequestsByPhone(eq("84912345678"), any())).thenReturn(1);
        when(userRepository.findByPhoneGlobal("84912345678")).thenReturn(Optional.of(user));
        when(otpRepository.save(any())).thenAnswer(i -> {
            PasswordResetOtp r = i.getArgument(0);
            r.setId(11L);
            return r;
        });

        String masked = service.requestOtp("0912345678", "1.2.3.4");

        assertThat(masked).contains("***");
        ArgumentCaptor<PasswordResetOtp> cap = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(otpRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(cap.getValue().getResendCount()).isEqualTo(1);
        assertThat(cap.getValue().getPhone()).isEqualTo("84912345678");
        verify(zaloZnsService).sendOtpSync(eq("84912345678"), anyString(), eq(11L));
    }

    @Test
    @DisplayName("requestOtp surfaces ZaloUserNotReachableException when the phone isn't on Zalo")
    void requestOtp_notOnZalo() {
        when(otpRepository.countRecentRequestsByPhone(anyString(), any())).thenReturn(0);
        when(userRepository.findByPhoneGlobal("84912345678")).thenReturn(Optional.of(user));
        when(otpRepository.save(any())).thenAnswer(i -> {
            PasswordResetOtp r = i.getArgument(0);
            r.setId(12L);
            return r;
        });
        doThrow(new ZaloUserNotReachableException("Zalo error -124"))
                .when(zaloZnsService).sendOtpSync(eq("84912345678"), anyString(), eq(12L));

        assertThatThrownBy(() -> service.requestOtp("0912345678", "ip"))
                .isInstanceOf(ZaloUserNotReachableException.class);

        // The OTP row is still saved (kept for rate-limiting via noRollbackFor)
        verify(otpRepository).save(any());
    }

    @Test
    @DisplayName("requestOtp surfaces ZaloSendException on a transient send failure")
    void requestOtp_sendFailed() {
        when(otpRepository.countRecentRequestsByPhone(anyString(), any())).thenReturn(0);
        when(userRepository.findByPhoneGlobal("84912345678")).thenReturn(Optional.of(user));
        when(otpRepository.save(any())).thenAnswer(i -> {
            PasswordResetOtp r = i.getArgument(0);
            r.setId(13L);
            return r;
        });
        doThrow(new ZaloSendException("Zalo ZNS call error"))
                .when(zaloZnsService).sendOtpSync(eq("84912345678"), anyString(), eq(13L));

        assertThatThrownBy(() -> service.requestOtp("0912345678", "ip"))
                .isInstanceOf(ZaloSendException.class);
    }

    @Test
    @DisplayName("requestOtp stays silent (no save) for an unregistered phone")
    void requestOtp_unregistered() {
        when(otpRepository.countRecentRequestsByPhone(anyString(), any())).thenReturn(0);
        when(userRepository.findByPhoneGlobal(anyString())).thenReturn(Optional.empty());

        String masked = service.requestOtp("0912345678", "ip");

        assertThat(masked).contains("***");
        verify(otpRepository, never()).save(any());
        verify(zaloZnsService, never()).sendOtpSync(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("requestOtp throws 429 when rate-limited")
    void requestOtp_rateLimited() {
        when(otpRepository.countRecentRequestsByPhone(anyString(), any())).thenReturn(3);
        assertThatThrownBy(() -> service.requestOtp("0912345678", "ip"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").hasToString("429 TOO_MANY_REQUESTS");
    }

    // ── verifyOtp ──────────────────────────────────────────────────────────────

    private PasswordResetOtp pendingOtp(String otp) {
        String salt = "abc123";
        PasswordResetOtp r = PasswordResetOtp.builder()
                .userId(user.getId()).phone("84912345678")
                .otpSalt(salt).otpHash(PasswordResetService.sha256(otp + salt))
                .status("PENDING").wrongAttempts(0).build();
        r.setId(11L);
        return r;
    }

    @Test
    @DisplayName("verifyOtp returns a reset token for a correct OTP")
    void verifyOtp_correct() {
        PasswordResetOtp record = pendingOtp("123456");
        when(otpRepository.findLatestPendingByPhone("84912345678")).thenReturn(Optional.of(record));

        String token = service.verifyOtp("0912345678", "123456");

        assertThat(token).isNotBlank();
        assertThat(record.getStatus()).isEqualTo("VERIFIED");
        assertThat(record.getResetTokenHash()).isEqualTo(PasswordResetService.sha256(token));
        assertThat(record.getTokenExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("verifyOtp throws when no pending OTP exists")
    void verifyOtp_none() {
        when(otpRepository.findLatestPendingByPhone(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyOtp("0912345678", "123456"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("verifyOtp on a wrong guess increments attempts and reports remaining")
    void verifyOtp_wrongGuess() {
        PasswordResetOtp record = pendingOtp("123456");
        when(otpRepository.findLatestPendingByPhone("84912345678")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.verifyOtp("0912345678", "000000"))
                .isInstanceOf(BadRequestException.class);
        assertThat(record.getWrongAttempts()).isEqualTo(1);
        assertThat(record.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("verifyOtp locks the OTP after the final wrong guess (429)")
    void verifyOtp_locksOnThirdWrong() {
        PasswordResetOtp record = pendingOtp("123456");
        record.setWrongAttempts(2);
        when(otpRepository.findLatestPendingByPhone("84912345678")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.verifyOtp("0912345678", "999999"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(record.getWrongAttempts()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo("LOCKED");
    }

    @Test
    @DisplayName("verifyOtp throws 429 when the OTP is already locked")
    void verifyOtp_alreadyLocked() {
        PasswordResetOtp record = pendingOtp("123456");
        record.setWrongAttempts(3);
        when(otpRepository.findLatestPendingByPhone("84912345678")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.verifyOtp("0912345678", "123456"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ── resetPassword ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword hashes the password, unlocks the account, marks the OTP used")
    void resetPassword_ok() {
        String token = "the-token";
        PasswordResetOtp record = pendingOtp("123456");
        record.setStatus("VERIFIED");
        when(otpRepository.findByResetTokenHash(PasswordResetService.sha256(token)))
                .thenReturn(Optional.of(record));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("ENC");

        service.resetPassword(token, "newpassword");

        assertThat(user.getPassword()).isEqualTo("ENC");
        assertThat(user.getAccountNonLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(record.getStatus()).isEqualTo("USED");
        assertThat(record.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword rejects a password shorter than 8 chars")
    void resetPassword_tooShort() {
        assertThatThrownBy(() -> service.resetPassword("t", "short"))
                .isInstanceOf(BadRequestException.class);
        verify(otpRepository, never()).findByResetTokenHash(anyString());
    }

    @Test
    @DisplayName("resetPassword rejects a null password")
    void resetPassword_null() {
        assertThatThrownBy(() -> service.resetPassword("t", null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resetPassword throws for an invalid token")
    void resetPassword_invalidToken() {
        when(otpRepository.findByResetTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("bad", "newpassword"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resetPassword throws when the user no longer exists")
    void resetPassword_userGone() {
        PasswordResetOtp record = pendingOtp("123456");
        when(otpRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(record));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("token", "newpassword"))
                .isInstanceOf(BadRequestException.class);
    }
}

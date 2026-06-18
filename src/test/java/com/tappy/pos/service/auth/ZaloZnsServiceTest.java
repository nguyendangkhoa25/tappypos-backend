package com.tappy.pos.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZaloZnsService Unit Tests")
class ZaloZnsServiceTest {

    @Mock private RestTemplate restTemplate;

    private ZaloZnsService service;

    @BeforeEach
    void setUp() {
        service = new ZaloZnsService(restTemplate);
        ReflectionTestUtils.setField(service, "accessToken", "tok");
        ReflectionTestUtils.setField(service, "templateId", "tmpl");
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    // ── static phone helpers ───────────────────────────────────────────────────

    @Test
    @DisplayName("normalizePhone converts 0-prefix to 84 and passes 84 through")
    void normalizePhone() {
        assertThat(ZaloZnsService.normalizePhone("0912345678")).isEqualTo("84912345678");
        assertThat(ZaloZnsService.normalizePhone("84912345678")).isEqualTo("84912345678");
        assertThat(ZaloZnsService.normalizePhone("+84 912 345 678")).isEqualTo("84912345678");
        assertThat(ZaloZnsService.normalizePhone(null)).isEmpty();
        assertThat(ZaloZnsService.normalizePhone("123")).isEqualTo("123");
    }

    @Test
    @DisplayName("maskPhone hides the middle and short numbers")
    void maskPhone() {
        assertThat(ZaloZnsService.maskPhone("84912345678")).isEqualTo("8491***678");
        assertThat(ZaloZnsService.maskPhone("123")).isEqualTo("****");
        assertThat(ZaloZnsService.maskPhone(null)).isEqualTo("****");
    }

    // ── sendOtpAsync ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendOtpAsync posts to ZNS when enabled and configured")
    void sendOtp_success() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "data", Map.of("message_id", "m1")));

        service.sendOtpAsync("0912345678", "123456", 9L);

        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("sendOtpAsync logs non-zero error responses without throwing")
    void sendOtp_nonZeroError() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 42, "message", "bad"));
        service.sendOtpAsync("0912345678", "123456", 9L);
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("sendOtpAsync swallows RestTemplate exceptions")
    void sendOtp_exception() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("network"));
        service.sendOtpAsync("0912345678", "123456", 9L);
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("sendOtpAsync does nothing when disabled")
    void sendOtp_disabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.sendOtpAsync("0912345678", "123456", 9L);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("sendOtpAsync does nothing when credentials missing")
    void sendOtp_notConfigured() {
        ReflectionTestUtils.setField(service, "accessToken", "");
        service.sendOtpAsync("0912345678", "123456", 9L);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    // ── sendAppointmentReminderAsync ─────────────────────────────────────────────

    @Test
    @DisplayName("appointment reminder uses the tenant token and template when provided")
    void reminder_tenantToken() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0));
        service.sendAppointmentReminderAsync("0912345678", "Lan", "Cắt tóc",
                "09:30", "25/05/2026", 5L, "tenantTmpl", "tenantToken");
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("appointment reminder falls back to the global token when tenant token blank")
    void reminder_globalFallback() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0));
        service.sendAppointmentReminderAsync("0912345678", "Lan", "Cắt tóc",
                "09:30", "25/05/2026", 5L, "tmpl", null);
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("appointment reminder skips when disabled")
    void reminder_disabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.sendAppointmentReminderAsync("0912345678", "Lan", "Cắt tóc",
                "09:30", "25/05/2026", 5L, "tmpl", null);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("appointment reminder skips when template id missing")
    void reminder_noTemplate() {
        service.sendAppointmentReminderAsync("0912345678", "Lan", "Cắt tóc",
                "09:30", "25/05/2026", 5L, null, "tenantToken");
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("appointment reminder swallows exceptions")
    void reminder_exception() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("network"));
        service.sendAppointmentReminderAsync("0912345678", "Lan", "Cắt tóc",
                "09:30", "25/05/2026", 5L, "tmpl", "tenantToken");
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    private static String anyUrl() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}

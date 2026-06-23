package com.tappy.pos.service.auth;

import com.tappy.pos.exception.ZaloSendException;
import com.tappy.pos.exception.ZaloUserNotReachableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("sendOtpAsync sends correct payload, phone normalized, headers set")
    void sendOtp_payloadAndHeaders() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "data", Map.of("message_id", "m1")));

        service.sendOtpAsync("0912345678", "123456", 9L);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyUrl(), captor.capture(), eq(Map.class));

        HttpEntity<?> entity = captor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).containsEntry("phone", "84912345678");
        assertThat(body).containsEntry("template_id", "tmpl");
        assertThat(body).containsEntry("tracking_id", "tappy-otp-9");
        @SuppressWarnings("unchecked")
        Map<String, Object> templateData = (Map<String, Object>) body.get("template_data");
        assertThat(templateData).containsEntry("otp", "123456");
        assertThat(templateData).containsEntry("expiry", "5 phút");

        HttpHeaders headers = entity.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(headers.getFirst("access_token")).isEqualTo("tok");
    }

    @Test
    @DisplayName("sendOtpAsync handles null result without throwing")
    void sendOtp_nullResult() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);
        service.sendOtpAsync("0912345678", "123456", 9L);
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    // ── sendOtpSync ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendOtpSync succeeds and returns on error=0")
    void sendOtpSync_success() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0, "data", Map.of("message_id", "m1")));

        service.sendOtpSync("0912345678", "123456", 7L);

        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("sendOtpSync handles error=0 with non-map data")
    void sendOtpSync_successNoData() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0));
        service.sendOtpSync("0912345678", "123456", 7L);
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("sendOtpSync returns silently (no throw) when disabled")
    void sendOtpSync_disabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.sendOtpSync("0912345678", "123456", 7L);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("sendOtpSync throws ZaloSendException when not configured")
    void sendOtpSync_notConfigured() {
        ReflectionTestUtils.setField(service, "templateId", "");
        assertThatThrownBy(() -> service.sendOtpSync("0912345678", "123456", 7L))
                .isInstanceOf(ZaloSendException.class);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("sendOtpSync wraps RestTemplate exceptions in ZaloSendException")
    void sendOtpSync_callError() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));
        assertThatThrownBy(() -> service.sendOtpSync("0912345678", "123456", 7L))
                .isInstanceOf(ZaloSendException.class)
                .hasMessageContaining("call error");
    }

    @Test
    @DisplayName("sendOtpSync throws ZaloUserNotReachableException on -124/-118/-134")
    void sendOtpSync_userNotReachable() {
        for (int code : new int[]{-124, -118, -134}) {
            RestTemplate rt = org.mockito.Mockito.mock(RestTemplate.class);
            ZaloZnsService svc = new ZaloZnsService(rt);
            ReflectionTestUtils.setField(svc, "accessToken", "tok");
            ReflectionTestUtils.setField(svc, "templateId", "tmpl");
            ReflectionTestUtils.setField(svc, "enabled", true);
            when(rt.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("error", code));
            assertThatThrownBy(() -> svc.sendOtpSync("0912345678", "123456", 7L))
                    .isInstanceOf(ZaloUserNotReachableException.class);
        }
    }

    @Test
    @DisplayName("sendOtpSync throws ZaloSendException on other non-zero error")
    void sendOtpSync_otherError() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 99, "message", "boom"));
        assertThatThrownBy(() -> service.sendOtpSync("0912345678", "123456", 7L))
                .isInstanceOf(ZaloSendException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("sendOtpSync throws ZaloSendException when result is null (error=-1)")
    void sendOtpSync_nullResult() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);
        assertThatThrownBy(() -> service.sendOtpSync("0912345678", "123456", 7L))
                .isInstanceOf(ZaloSendException.class);
    }

    @Test
    @DisplayName("sendOtpSync treats non-numeric error field as error (-1) and throws")
    void sendOtpSync_nonNumericError() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("error", "not-a-number");
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(resp);
        assertThatThrownBy(() -> service.sendOtpSync("0912345678", "123456", 7L))
                .isInstanceOf(ZaloSendException.class);
    }

    // ── sendPawnDueReminderAsync ─────────────────────────────────────────────────

    @Test
    @DisplayName("pawn reminder uses tenant token and template when provided, sends payload")
    void pawnReminder_tenantToken() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0));

        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", "tenantToken");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyUrl(), captor.capture(), eq(Map.class));
        HttpEntity<?> entity = captor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).containsEntry("phone", "84912345678");
        assertThat(body).containsEntry("template_id", "pawnTmpl");
        assertThat(body).containsEntry("tracking_id", "tappy-pawn-due-3");
        @SuppressWarnings("unchecked")
        Map<String, Object> td = (Map<String, Object>) body.get("template_data");
        assertThat(td).containsEntry("customer_name", "Lan");
        assertThat(td).containsEntry("amount", "5.000.000 ₫");
        assertThat(td).containsEntry("date", "25/06/2026");
        assertThat(entity.getHeaders().getFirst("access_token")).isEqualTo("tenantToken");
    }

    @Test
    @DisplayName("pawn reminder falls back to global token when tenant token blank")
    void pawnReminder_globalFallback() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 0));
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", "   ");
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyUrl(), captor.capture(), eq(Map.class));
        assertThat(captor.getValue().getHeaders().getFirst("access_token")).isEqualTo("tok");
    }

    @Test
    @DisplayName("pawn reminder skips when disabled")
    void pawnReminder_disabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", "tenantToken");
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("pawn reminder skips when no token resolvable")
    void pawnReminder_noToken() {
        ReflectionTestUtils.setField(service, "accessToken", "");
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", null);
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("pawn reminder skips when template id missing")
    void pawnReminder_noTemplate() {
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, null, "tenantToken");
        verify(restTemplate, never()).postForObject(anyUrl(), any(), any());
    }

    @Test
    @DisplayName("pawn reminder logs non-zero error without throwing")
    void pawnReminder_nonZeroError() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", 7, "message", "bad"));
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", "tenantToken");
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("pawn reminder swallows exceptions")
    void pawnReminder_exception() {
        when(restTemplate.postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("network"));
        service.sendPawnDueReminderAsync("0912345678", "Lan", "5.000.000 ₫",
                "25/06/2026", 3L, "pawnTmpl", "tenantToken");
        verify(restTemplate).postForObject(anyUrl(), any(HttpEntity.class), eq(Map.class));
    }

    private static String anyUrl() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}

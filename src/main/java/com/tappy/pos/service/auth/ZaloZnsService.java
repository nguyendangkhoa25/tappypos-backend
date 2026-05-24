package com.tappy.pos.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Zalo ZNS (Zalo Notification Service) sender for transactional OTP messages.
 *
 * The OTP template must be pre-approved on the Zalo OA portal.
 * Template variables: {{otp}} (6-digit code), {{expiry}} (e.g. "5 phút").
 *
 * Config keys (set via environment variables):
 *   ZALO_ZNS_ACCESS_TOKEN  — long-lived OA access token from Zalo Developer
 *   ZALO_ZNS_TEMPLATE_ID   — approved ZNS template ID
 *   ZALO_ZNS_ENABLED       — set to 'false' in dev to print OTP to logs only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloZnsService {

    private static final String ZNS_URL = "https://business.openapi.zalo.me/message/template";

    @Value("${zalo.zns.access-token:}")
    private String accessToken;

    @Value("${zalo.zns.template-id:}")
    private String templateId;

    @Value("${zalo.zns.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    /**
     * Send OTP message via Zalo ZNS. Fire-and-forget (@Async).
     * Any exception is swallowed so the caller's transaction is never affected.
     *
     * @param phone raw phone number (0XXXXXXXXX or 84XXXXXXXXX)
     * @param otp   6-digit OTP string
     * @param otpId DB row id — logged for correlation
     */
    @Async
    public void sendOtpAsync(String phone, String otp, Long otpId) {
        if (!enabled) {
            log.info("[ZNS-DISABLED] OTP for rowId={} would be sent to {} — code: {}",
                    otpId, maskPhone(phone), otp);
            return;
        }

        if (accessToken.isBlank() || templateId.isBlank()) {
            log.error("[ZNS] accessToken or templateId not configured — OTP NOT sent (rowId={})", otpId);
            return;
        }

        String intlPhone = normalizePhone(phone);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_token", accessToken);

            Map<String, Object> body = Map.of(
                    "phone", intlPhone,
                    "template_id", templateId,
                    "template_data", Map.of("otp", otp, "expiry", "5 phút"),
                    "tracking_id", "tappy-otp-" + otpId
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    ZNS_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (result != null && Integer.valueOf(0).equals(result.get("error"))) {
                Object msgData = result.get("data");
                String msgId = (msgData instanceof Map<?, ?> m) ? String.valueOf(m.get("message_id")) : null;
                log.info("[ZNS] OTP sent — phone={} rowId={} messageId={}", maskPhone(intlPhone), otpId, msgId);
            } else {
                log.warn("[ZNS] Non-zero error response for phone={} rowId={}: {}", maskPhone(intlPhone), otpId, result);
            }
        } catch (Exception e) {
            log.error("[ZNS] Send failed for phone={} rowId={}: {}", maskPhone(intlPhone), otpId, e.getMessage(), e);
        }
    }

    /**
     * Send an appointment reminder via Zalo ZNS. Fire-and-forget (@Async).
     * Template variables: {{customer_name}}, {{services}}, {{time}}, {{date}}.
     *
     * @param phone              customer phone (0XXXXXXXXX or 84XXXXXXXXX)
     * @param customerName       customer's display name
     * @param services           comma-separated service names
     * @param time               formatted appointment time, e.g. "09:30"
     * @param date               formatted appointment date, e.g. "25/05/2026"
     * @param appointmentId      DB row id — logged for correlation
     * @param templateId         the Zalo ZNS template ID to use (tenant default or global fallback)
     * @param tenantAccessToken  tenant's own OA access token, or {@code null} to use the platform global token
     */
    @Async
    public void sendAppointmentReminderAsync(
            String phone, String customerName, String services,
            String time, String date, Long appointmentId, String templateId,
            String tenantAccessToken) {
        if (!enabled) {
            log.info("[ZNS-DISABLED] Appointment reminder for apptId={} would be sent to {} — {}/{} {} (templateId={})",
                    appointmentId, maskPhone(phone), services, time, date, templateId);
            return;
        }

        // Resolve which access token to use: tenant-specific > global platform
        String effectiveToken = (tenantAccessToken != null && !tenantAccessToken.isBlank())
                ? tenantAccessToken : accessToken;

        if (effectiveToken.isBlank() || templateId == null || templateId.isBlank()) {
            log.warn("[ZNS] accessToken or templateId not configured — reminder NOT sent (apptId={})", appointmentId);
            return;
        }

        String intlPhone = normalizePhone(phone);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_token", effectiveToken);

            Map<String, Object> body = Map.of(
                    "phone", intlPhone,
                    "template_id", templateId,
                    "template_data", Map.of(
                            "customer_name", customerName,
                            "services", services,
                            "time", time,
                            "date", date
                    ),
                    "tracking_id", "tappy-reminder-" + appointmentId
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    ZNS_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (result != null && Integer.valueOf(0).equals(result.get("error"))) {
                log.info("[ZNS] Reminder sent — phone={} apptId={}", maskPhone(intlPhone), appointmentId);
            } else {
                log.warn("[ZNS] Non-zero error for reminder phone={} apptId={}: {}",
                        maskPhone(intlPhone), appointmentId, result);
            }
        } catch (Exception e) {
            log.error("[ZNS] Reminder failed for phone={} apptId={}: {}",
                    maskPhone(intlPhone), appointmentId, e.getMessage(), e);
        }
    }

    /**
     * Converts 0XXXXXXXXX → 84XXXXXXXXX.
     * Numbers already starting with 84 pass through unchanged.
     */
    static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("84")) return digits;
        if (digits.startsWith("0")) return "84" + digits.substring(1);
        return digits;
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) return "****";
        int keep = Math.min(4, phone.length() - 3);
        return phone.substring(0, keep) + "***" + phone.substring(phone.length() - 3);
    }
}

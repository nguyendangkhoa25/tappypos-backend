package com.tappy.pos.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for the central <strong>Tappy Message</strong> service ({@code msg.tappy.vn/api/v1}), which
 * delivers OTP and marketing/notification messages over Zalo (and later LINE / WhatsApp) on behalf of
 * all Tappy apps and keeps a full audit log. POS no longer talks to Zalo directly — it authenticates
 * to this service with its per-app {@code X-API-Key} and lets the service own the OA credentials.
 *
 * <ul>
 *   <li><b>OTP</b> ({@code POST /messages/otp}) is synchronous — the response carries the provider
 *       outcome inline. Used by the interactive password-reset flow.</li>
 *   <li><b>Reminders</b> ({@code POST /messages/marketing}) are accepted asynchronously by the
 *       service (202 + PENDING). We additionally fire them {@code @Async} so a scheduler tick never
 *       blocks on the HTTP round-trip.</li>
 * </ul>
 *
 * <h3>Error handling</h3>
 * Every send is best-effort and never throws to the caller. Two failure shapes are handled distinctly
 * and logged:
 * <ul>
 *   <li><b>Transport / HTTP error</b> — a non-2xx status ({@code 401} bad/missing key, {@code 400}
 *       validation, {@code 429} rate limited) makes {@code RestTemplate} throw
 *       {@link HttpStatusCodeException}; we capture the status and the {@code {success,message}} body.</li>
 *   <li><b>Provider rejection</b> — a {@code 200} whose envelope is {@code data.status=FAILED} with a
 *       {@code data.errorCode} (e.g. {@code ZALO_USER_NOT_REACHABLE}).</li>
 * </ul>
 * Each path returns a {@link Result} so callers can react (the password-reset flow logs it but, by
 * design, still returns an identical anti-enumeration response).
 *
 * Config keys (set via environment variables):
 * <pre>
 *   tappy.message.enabled    — false in dev to skip the real call (OTP printed to logs)
 *   tappy.message.base-url   — e.g. https://msg.tappy.vn/api/v1
 *   tappy.message.api-key    — this app's X-API-Key (provisioned via the message service admin)
 *   tappy.message.channel    — ZALO | LINE | WHATSAPP (default ZALO)
 *   tappy.message.otp-template-id                  — optional; blank lets the service use its default
 *   tappy.message.appointment-reminder-template-id — Zalo ZNS template for appointment reminders
 *   tappy.message.pawn-due-reminder-template-id    — Zalo ZNS template for pawn due reminders
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TappyMessageClient {

    private static final String OTP_PATH = "/messages/otp";
    private static final String MARKETING_PATH = "/messages/marketing";
    private static final String API_KEY_HEADER = "X-API-Key";

    /** Outcome of a send. {@code errorCode} is the provider/transport error when {@code !sent}. */
    public record Result(boolean sent, String errorCode) {
        static Result ok() {
            return new Result(true, null);
        }

        static Result failed(String errorCode) {
            return new Result(false, errorCode);
        }
    }

    @Value("${tappy.message.enabled:false}")
    private boolean enabled;

    @Value("${tappy.message.base-url:https://msg.tappy.vn/api/v1}")
    private String baseUrl;

    @Value("${tappy.message.api-key:}")
    private String apiKey;

    @Value("${tappy.message.channel:ZALO}")
    private String channel;

    @Value("${tappy.message.otp-template-id:}")
    private String otpTemplateId;

    @Value("${tappy.message.appointment-reminder-template-id:}")
    private String appointmentReminderTemplateId;

    @Value("${tappy.message.pawn-due-reminder-template-id:}")
    private String pawnDueReminderTemplateId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Send a password-reset OTP synchronously via the Tappy Message service. Best-effort: any
     * delivery problem is logged and reflected in the returned {@link Result}, never thrown — so the
     * caller's anti-enumeration response stays identical regardless of outcome.
     *
     * @param phone raw phone number (0XXXXXXXXX or 84XXXXXXXXX)
     * @param otp   the 6-digit code the caller generated
     * @param otpId DB row id — used as the correlation {@code requestId} and logged
     * @return the send outcome (never {@code null})
     */
    public Result sendOtp(String phone, String otp, Long otpId) {
        String masked = PhoneUtil.maskPhone(phone);
        if (!enabled) {
            log.info("[MSG-DISABLED] OTP for rowId={} would be sent to {} — code: {}", otpId, masked, otp);
            return Result.ok();
        }
        if (apiKey.isBlank()) {
            log.error("[MSG] tappy.message.api-key not configured — OTP NOT sent (rowId={})", otpId);
            return Result.failed("NOT_CONFIGURED");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("channel", channel);
        body.put("recipient", PhoneUtil.normalizePhone(phone));
        body.put("otp", otp);
        if (!otpTemplateId.isBlank()) {
            body.put("templateId", otpTemplateId);
        }
        body.put("params", Map.of("expiry", "5 phút"));
        body.put("requestId", "tappy-otp-" + otpId);

        return post(OTP_PATH, body, "OTP", masked, "rowId=" + otpId);
    }

    /**
     * Send an appointment reminder via the Tappy Message marketing channel. Fire-and-forget.
     * Skipped (with a debug log) when no appointment-reminder template is configured.
     */
    @Async
    public void sendAppointmentReminder(String phone, String customerName, String services,
                                        String time, String date, Long appointmentId) {
        if (appointmentReminderTemplateId.isBlank()) {
            log.debug("[MSG] No appointment-reminder template configured — reminder NOT sent (apptId={})",
                    appointmentId);
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("customer_name", nullToEmpty(customerName));
        params.put("services", nullToEmpty(services));
        params.put("time", nullToEmpty(time));
        params.put("date", nullToEmpty(date));
        sendMarketing(phone, appointmentReminderTemplateId, params,
                "tappy-reminder-" + appointmentId, "reminder", "apptId=" + appointmentId);
    }

    /**
     * Send a pawn-contract due-date reminder via the Tappy Message marketing channel. Fire-and-forget.
     * Skipped (with a debug log) when no pawn-due-reminder template is configured.
     */
    @Async
    public void sendPawnDueReminder(String phone, String customerName, String amount,
                                    String date, Long pawnId) {
        if (pawnDueReminderTemplateId.isBlank()) {
            log.debug("[MSG] No pawn-due-reminder template configured — reminder NOT sent (pawnId={})", pawnId);
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("customer_name", nullToEmpty(customerName));
        params.put("amount", nullToEmpty(amount));
        params.put("date", nullToEmpty(date));
        sendMarketing(phone, pawnDueReminderTemplateId, params,
                "tappy-pawn-due-" + pawnId, "pawn reminder", "pawnId=" + pawnId);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private Result sendMarketing(String phone, String templateId, Map<String, String> params,
                                 String requestId, String kind, String correlation) {
        String masked = PhoneUtil.maskPhone(phone);
        if (!enabled) {
            log.info("[MSG-DISABLED] {} for {} would be sent to {} (template={})",
                    kind, correlation, masked, templateId);
            return Result.ok();
        }
        if (apiKey.isBlank()) {
            log.warn("[MSG] tappy.message.api-key not configured — {} NOT sent ({})", kind, correlation);
            return Result.failed("NOT_CONFIGURED");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("channel", channel);
        body.put("recipient", PhoneUtil.normalizePhone(phone));
        body.put("templateId", templateId);
        body.put("params", params);
        body.put("requestId", requestId);

        return post(MARKETING_PATH, body, kind, masked, correlation);
    }

    /**
     * POSTs to the message service and maps the outcome to a {@link Result}, handling both transport
     * errors (non-2xx → {@link HttpStatusCodeException}) and the {@code data.status=FAILED} envelope.
     * Never throws.
     */
    private Result post(String path, Map<String, Object> body, String kind, String maskedPhone, String correlation) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    baseUrl + path, new HttpEntity<>(body, headers()), Map.class);
            return interpret(kind, maskedPhone, correlation, resp);
        } catch (HttpStatusCodeException e) {
            // 401 (bad/missing key), 400 (validation), 429 (rate limited) — capture status + body.
            String message = extractMessage(e.getResponseBodyAsString());
            log.error("[MSG] {} HTTP {} for phone={} {}: {}",
                    kind, e.getStatusCode().value(), maskedPhone, correlation, message);
            return Result.failed("HTTP_" + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("[MSG] {} transport error for phone={} {}: {}", kind, maskedPhone, correlation, e.getMessage());
            return Result.failed("TRANSPORT_ERROR");
        }
    }

    /** Interprets the {@code {success, data:{status, errorCode, id}, message}} envelope. */
    private Result interpret(String kind, String maskedPhone, String correlation, Map<String, Object> resp) {
        Object data = resp == null ? null : resp.get("data");
        if (!(data instanceof Map<?, ?> m)) {
            log.warn("[MSG] {} — unexpected response for phone={} {}: {}", kind, maskedPhone, correlation, resp);
            return Result.failed("BAD_RESPONSE");
        }
        Object status = m.get("status");
        if ("FAILED".equals(status)) {
            String errorCode = String.valueOf(m.get("errorCode"));
            log.warn("[MSG] {} FAILED — phone={} {} errorCode={}", kind, maskedPhone, correlation, errorCode);
            return Result.failed(errorCode);
        }
        log.info("[MSG] {} {} — phone={} {} messageId={}", kind, status, maskedPhone, correlation, m.get("id"));
        return Result.ok();
    }

    /** Pulls the {@code message} field out of an error envelope for logging; falls back to the raw body. */
    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "(no body)";
        }
        try {
            Object message = objectMapper.readValue(responseBody, Map.class).get("message");
            return message != null ? String.valueOf(message) : responseBody;
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(API_KEY_HEADER, apiKey);
        return headers;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

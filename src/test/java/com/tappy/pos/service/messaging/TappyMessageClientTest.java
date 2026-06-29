package com.tappy.pos.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TappyMessageClient Unit Tests")
class TappyMessageClientTest {

    private static final String BASE = "https://msg.test/api/v1";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TappyMessageClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new TappyMessageClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "baseUrl", BASE);
        ReflectionTestUtils.setField(client, "apiKey", "tmsg_test_key");
        ReflectionTestUtils.setField(client, "channel", "ZALO");
        ReflectionTestUtils.setField(client, "otpTemplateId", "");
        ReflectionTestUtils.setField(client, "appointmentReminderTemplateId", "");
        ReflectionTestUtils.setField(client, "pawnDueReminderTemplateId", "");
    }

    @Test
    @DisplayName("sendOtp posts to /messages/otp with the API key and a SENT result")
    void sendOtp_sent() {
        server.expect(requestTo(BASE + "/messages/otp"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-API-Key", "tmsg_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.channel").value("ZALO"))
                .andExpect(jsonPath("$.recipient").value("84912345678"))
                .andExpect(jsonPath("$.otp").value("123456"))
                .andExpect(jsonPath("$.requestId").value("tappy-otp-7"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":\"m-1\",\"status\":\"SENT\"}}",
                        MediaType.APPLICATION_JSON));

        TappyMessageClient.Result result = client.sendOtp("0912345678", "123456", 7L);

        assertThat(result.sent()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("sendOtp surfaces a FAILED envelope (200) as a failed Result with the errorCode")
    void sendOtp_providerFailed() {
        server.expect(requestTo(BASE + "/messages/otp"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":\"m-2\",\"status\":\"FAILED\","
                                + "\"errorCode\":\"ZALO_USER_NOT_REACHABLE\"}}",
                        MediaType.APPLICATION_JSON));

        TappyMessageClient.Result result = client.sendOtp("0912345678", "123456", 8L);

        assertThat(result.sent()).isFalse();
        assertThat(result.errorCode()).isEqualTo("ZALO_USER_NOT_REACHABLE");
        server.verify();
    }

    @Test
    @DisplayName("sendOtp maps a transport error (429) to a failed Result without throwing")
    void sendOtp_httpError() {
        server.expect(requestTo(BASE + "/messages/otp"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"success\":false,\"message\":\"rate limited\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        TappyMessageClient.Result result = client.sendOtp("0912345678", "123456", 9L);

        assertThat(result.sent()).isFalse();
        assertThat(result.errorCode()).isEqualTo("HTTP_429");
        server.verify();
    }

    @Test
    @DisplayName("sendOtp short-circuits (no HTTP call) when disabled")
    void sendOtp_disabled() {
        ReflectionTestUtils.setField(client, "enabled", false);
        // No server expectation — any HTTP call would fail verification.

        TappyMessageClient.Result result = client.sendOtp("0912345678", "123456", 10L);

        assertThat(result.sent()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("sendOtp returns NOT_CONFIGURED when the API key is blank")
    void sendOtp_noApiKey() {
        ReflectionTestUtils.setField(client, "apiKey", "");

        TappyMessageClient.Result result = client.sendOtp("0912345678", "123456", 11L);

        assertThat(result.sent()).isFalse();
        assertThat(result.errorCode()).isEqualTo("NOT_CONFIGURED");
        server.verify();
    }

    @Test
    @DisplayName("sendAppointmentReminder posts to /messages/marketing with the configured template")
    void appointmentReminder_sends() {
        ReflectionTestUtils.setField(client, "appointmentReminderTemplateId", "appt-tmpl");
        server.expect(requestTo(BASE + "/messages/marketing"))
                .andExpect(header("X-API-Key", "tmsg_test_key"))
                .andExpect(jsonPath("$.templateId").value("appt-tmpl"))
                .andExpect(jsonPath("$.recipient").value("84900000000"))
                .andExpect(jsonPath("$.params.customer_name").value("Khách A"))
                .andExpect(jsonPath("$.requestId").value("tappy-reminder-3"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":\"m-3\",\"status\":\"PENDING\"}}",
                        MediaType.APPLICATION_JSON));

        client.sendAppointmentReminder("0900000000", "Khách A", "Cắt tóc", "10:00", "01/06/2026", 3L);

        server.verify();
    }

    @Test
    @DisplayName("sendAppointmentReminder is skipped (no HTTP call) when no template is configured")
    void appointmentReminder_noTemplate_skips() {
        // appointmentReminderTemplateId left blank — no server expectation.
        client.sendAppointmentReminder("0900000000", "Khách A", "Cắt tóc", "10:00", "01/06/2026", 4L);
        server.verify();
    }
}

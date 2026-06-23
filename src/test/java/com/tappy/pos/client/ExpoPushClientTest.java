package com.tappy.pos.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpoPushClientTest {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExpoPushClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "enabled", true);
    }

    @Test
    void doesNothingWhenDisabled() {
        ReflectionTestUtils.setField(client, "enabled", false);

        client.sendAsync(List.of("ExponentPushToken[abc]"), "T", "B", null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void doesNothingWhenTokensNull() {
        client.sendAsync(null, "T", "B", null);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void doesNothingWhenTokensEmpty() {
        client.sendAsync(List.of(), "T", "B", null);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void doesNothingWhenNoValidTokens() {
        client.sendAsync(Arrays.asList("garbage", "fcm-token-xyz", null), "T", "B", null);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsSingleBatchForValidTokens() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        client.sendAsync(List.of("ExponentPushToken[abc]", "ExponentPushToken[def]"),
                "Title", "Body", null);

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));

        List<Map<String, Object>> messages = captor.getValue().getBody();
        assertThat(messages).hasSize(2);
        Map<String, Object> first = messages.get(0);
        assertThat(first.get("to")).isEqualTo("ExponentPushToken[abc]");
        assertThat(first.get("title")).isEqualTo("Title");
        assertThat(first.get("body")).isEqualTo("Body");
        assertThat(first.get("sound")).isEqualTo("default");
        assertThat(first).doesNotContainKey("data");

        HttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(headers.getAccept()).contains(MediaType.APPLICATION_JSON);
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtersOutInvalidTokensKeepingOnlyExponentTokens() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        client.sendAsync(Arrays.asList("ExponentPushToken[abc]", "bad", null, "fcm:xyz"),
                "T", "B", null);

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));

        List<Map<String, Object>> messages = captor.getValue().getBody();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).get("to")).isEqualTo("ExponentPushToken[abc]");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deduplicatesTokens() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        client.sendAsync(List.of("ExponentPushToken[abc]", "ExponentPushToken[abc]"),
                "T", "B", null);

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));
        assertThat(captor.getValue().getBody()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void includesDataPayloadWhenPresent() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));
        Map<String, Object> data = Map.of("screen", "orders", "id", "42");

        client.sendAsync(List.of("ExponentPushToken[abc]"), "T", "B", data);

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));
        assertThat(captor.getValue().getBody().get(0).get("data")).isEqualTo(data);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyDataMapIsNotIncluded() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        client.sendAsync(List.of("ExponentPushToken[abc]"), "T", "B", Map.of());

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));
        assertThat(captor.getValue().getBody().get(0)).doesNotContainKey("data");
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchesTokensOverHundredIntoMultiplePosts() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            tokens.add("ExponentPushToken[" + i + "]");
        }

        client.sendAsync(tokens, "T", "B", null);

        // 250 tokens / 100 per batch = 3 posts (100, 100, 50)
        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(3)).postForEntity(eq(SEND_URL), captor.capture(), eq(String.class));

        List<HttpEntity<List<Map<String, Object>>>> calls = captor.getAllValues();
        assertThat(calls.get(0).getBody()).hasSize(100);
        assertThat(calls.get(1).getBody()).hasSize(100);
        assertThat(calls.get(2).getBody()).hasSize(50);
    }

    @Test
    void swallowsExceptionFromRestTemplate() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("expo unreachable"));

        assertThatCode(() ->
                client.sendAsync(List.of("ExponentPushToken[abc]"), "T", "B", null))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void continuesRemainingBatchesAfterOneFails() {
        when(restTemplate.postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("first fails"))
                .thenReturn(ResponseEntity.ok("[]"));

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            tokens.add("ExponentPushToken[" + i + "]");
        }

        assertThatCode(() -> client.sendAsync(tokens, "T", "B", null)).doesNotThrowAnyException();

        // Both batches attempted even though the first threw.
        verify(restTemplate, times(2)).postForEntity(eq(SEND_URL), any(HttpEntity.class), eq(String.class));
    }
}

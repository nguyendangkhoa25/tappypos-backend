package com.tappy.pos.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends push notifications through the Expo Push API
 * (<a href="https://docs.expo.dev/push-notifications/sending-notifications/">docs</a>).
 *
 * Fire-and-forget: {@link #sendAsync} runs off the request thread and never throws into the
 * caller — a failed push must not affect the in-app notification or the originating request.
 * Does no DB access, so it needs no tenant context; callers resolve tokens first and pass them in.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpoPushClient {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";
    private static final int BATCH_SIZE = 100; // Expo accepts up to 100 messages per request

    private final RestTemplate restTemplate;

    @Value("${expo.push.enabled:true}")
    private boolean enabled;

    /**
     * Deliver one notification to many device tokens. Tokens are batched per Expo's limit.
     * @param data optional payload for deep-linking when the user taps the notification
     */
    @Async
    public void sendAsync(List<String> tokens, String title, String body, Map<String, Object> data) {
        if (!enabled || tokens == null || tokens.isEmpty()) return;

        // De-dup; Expo tokens look like ExponentPushToken[xxx] — skip anything obviously invalid.
        List<String> valid = tokens.stream()
                .filter(t -> t != null && t.startsWith("ExponentPushToken"))
                .distinct()
                .toList();
        if (valid.isEmpty()) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        for (int i = 0; i < valid.size(); i += BATCH_SIZE) {
            List<String> batch = valid.subList(i, Math.min(i + BATCH_SIZE, valid.size()));
            List<Map<String, Object>> messages = new ArrayList<>(batch.size());
            for (String token : batch) {
                Map<String, Object> m = new HashMap<>();
                m.put("to", token);
                m.put("title", title);
                m.put("body", body);
                m.put("sound", "default");
                if (data != null && !data.isEmpty()) m.put("data", data);
                messages.add(m);
            }
            try {
                restTemplate.postForEntity(SEND_URL, new HttpEntity<>(messages, headers), String.class);
            } catch (Exception e) {
                log.warn("Expo push send failed for {} token(s): {}", batch.size(), e.getMessage());
            }
        }
    }
}

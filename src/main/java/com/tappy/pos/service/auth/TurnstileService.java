package com.tappy.pos.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Verifies Cloudflare Turnstile tokens server-side.
 * Set cloudflare.turnstile.enabled=false in dev to skip verification.
 */
@Service
@Slf4j
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${cloudflare.turnstile.secret:}")
    private String secretKey;

    @Value("${cloudflare.turnstile.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verify(String token, String clientIp) {
        if (!enabled) {
            return true;
        }
        if (token == null || token.isBlank()) {
            log.warn("Turnstile token is missing");
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);
            if (clientIp != null && !clientIp.isBlank()) {
                body.add("remoteip", clientIp);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                VERIFY_URL,
                new HttpEntity<>(body, headers),
                Map.class
            );

            boolean success = result != null && Boolean.TRUE.equals(result.get("success"));
            if (!success) {
                log.warn("Turnstile verification failed: {}", result);
            }
            return success;
        } catch (Exception e) {
            log.error("Turnstile verification error: {}", e.getMessage());
            return false;
        }
    }
}

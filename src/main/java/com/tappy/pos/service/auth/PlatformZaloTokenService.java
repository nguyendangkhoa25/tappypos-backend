package com.tappy.pos.service.auth;

import com.tappy.pos.model.entity.integration.ZaloZnsCredential;
import com.tappy.pos.repository.integration.ZaloZnsCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Supplies a always-fresh access token for the <strong>platform</strong> Zalo Official
 * Account ("Tappy Việt Nam") used to send password-reset OTP.
 *
 * <p>Unlike {@link com.tappy.pos.service.tenant.ZaloOaService} (which manages a
 * <em>per-tenant</em> OA token in the RLS-scoped {@code shop_config}), this service works
 * with <em>no tenant context</em> — the forgot-password flow has none — and persists the
 * single platform credential in the master {@code zalo_zns_credential} table.
 *
 * <p>Token flow (Zalo OA v4):
 * <pre>
 *   POST https://oauth.zaloapp.com/v4/oa/access_token
 *   Header: secret_key: {appSecret}
 *   Body (form): grant_type=refresh_token&amp;app_id={appId}&amp;refresh_token={refreshToken}
 *   Response: { access_token, refresh_token (rotated), expires_in (seconds) }
 * </pre>
 *
 * The access token is refreshed {@value #EXPIRY_BUFFER_SECONDS}s ahead of expiry; the
 * rotating (single-use) refresh token is written back under a pessimistic row lock so
 * concurrent OTP sends never refresh in parallel and invalidate each other's token.
 *
 * <p>Bootstrapping: the first row is seeded from {@code zalo.zns.app-id} /
 * {@code zalo.zns.app-secret} / {@code zalo.zns.bootstrap-refresh-token} (one-time env
 * vars from the OA authorization). Once seeded the service is self-sustaining and the
 * bootstrap refresh token env var can be removed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformZaloTokenService {

    private static final String TOKEN_URL = "https://oauth.zaloapp.com/v4/oa/access_token";
    /** Refresh this many seconds before actual expiry so we never hand out a near-dead token. */
    private static final int EXPIRY_BUFFER_SECONDS = 300;
    /** Fallback token TTL (seconds) when Zalo omits {@code expires_in} — Zalo OA access tokens last ~25h. */
    private static final int DEFAULT_EXPIRES_IN = 90_000;

    private final ZaloZnsCredentialRepository repository;
    private final RestTemplate restTemplate;

    @Value("${zalo.zns.app-id:}")
    private String bootstrapAppId;

    @Value("${zalo.zns.app-secret:}")
    private String bootstrapAppSecret;

    @Value("${zalo.zns.bootstrap-refresh-token:}")
    private String bootstrapRefreshToken;

    /**
     * Returns a valid platform OA access token, refreshing (and persisting the rotated
     * refresh token) if the stored one is near expiry. Seeds the credential from the
     * bootstrap env vars on first use.
     *
     * @return a usable access token, or {@code null} when the platform OA is not configured
     *         or a refresh failed — the caller should then fall back (e.g. dev/disabled mode).
     */
    // REQUIRES_NEW: a Zalo refresh rotates the single-use refresh token server-side — an
    // irreversible side effect. Committing in our own transaction guarantees the rotated token is
    // persisted even if the surrounding request (e.g. the OTP send) later fails and rolls back;
    // otherwise a transient send error would discard the new token and permanently brick refresh.
    // Safe here: zalo_zns_credential is a master table with no RLS and this flow has no tenant context.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getAccessToken() {
        ZaloZnsCredential cred = repository.findFirstByDeletedFalseOrderByIdAsc().orElse(null);
        if (cred == null) {
            cred = bootstrap();
            if (cred == null) {
                return null; // platform OA not configured
            }
        }

        if (cred.getAccessToken() != null && cred.getTokenExpiry() != null
                && LocalDateTime.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(cred.getTokenExpiry())) {
            return cred.getAccessToken();
        }

        return refresh(cred);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private ZaloZnsCredential bootstrap() {
        if (isBlank(bootstrapAppId) || isBlank(bootstrapAppSecret) || isBlank(bootstrapRefreshToken)) {
            log.warn("[ZNS-TOKEN] Platform Zalo OA not configured — set zalo.zns.app-id / app-secret / "
                    + "bootstrap-refresh-token to enable refreshing OTP tokens");
            return null;
        }
        ZaloZnsCredential cred = ZaloZnsCredential.builder()
                .appId(bootstrapAppId.trim())
                .appSecret(bootstrapAppSecret.trim())
                .refreshToken(bootstrapRefreshToken.trim())
                .tokenExpiry(LocalDateTime.now().minusSeconds(1)) // force an immediate refresh
                .build();
        cred = repository.saveAndFlush(cred);
        log.info("[ZNS-TOKEN] Platform Zalo OA credential bootstrapped for appId={}", cred.getAppId());
        return cred;
    }

    private String refresh(ZaloZnsCredential cred) {
        if (isBlank(cred.getRefreshToken())) {
            log.error("[ZNS-TOKEN] No refresh token stored for appId={} — cannot refresh", cred.getAppId());
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("secret_key", cred.getAppSecret());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("app_id", cred.getAppId());
            body.add("refresh_token", cred.getRefreshToken());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

            if (response == null) {
                log.error("[ZNS-TOKEN] Empty response from Zalo token endpoint for appId={}", cred.getAppId());
                return null;
            }
            Object error = response.get("error");
            if (error != null && !Integer.valueOf(0).equals(error)) {
                log.error("[ZNS-TOKEN] Zalo refresh rejected for appId={}: error={} message={}",
                        cred.getAppId(), error, response.get("message"));
                return null;
            }

            String accessToken = (String) response.get("access_token");
            if (isBlank(accessToken)) {
                log.error("[ZNS-TOKEN] Zalo returned no access_token for appId={}: {}", cred.getAppId(), response);
                return null;
            }
            String newRefresh = (String) response.getOrDefault("refresh_token", cred.getRefreshToken());
            int expiresIn = parseExpiresIn(response.get("expires_in"));

            cred.setAccessToken(accessToken);
            cred.setRefreshToken(newRefresh);
            cred.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            repository.save(cred);

            log.info("[ZNS-TOKEN] Platform Zalo OA token refreshed for appId={} (expires in {}s)",
                    cred.getAppId(), expiresIn);
            return accessToken;
        } catch (Exception e) {
            log.error("[ZNS-TOKEN] Platform Zalo OA token refresh failed for appId={}: {}",
                    cred.getAppId(), e.getMessage(), e);
            return null;
        }
    }

    private static int parseExpiresIn(Object raw) {
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) { /* fall through */ }
        }
        return DEFAULT_EXPIRES_IN;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.tenant.ConnectZaloOaRequest;
import com.tappy.pos.model.dto.tenant.ZaloOaStatusDTO;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Manages the per-tenant Zalo Official Account connection.
 *
 * <p>Zalo ZNS token flow (App-level grant):
 * <pre>
 *   POST https://oauth.zaloapp.com/v4/oa/access_token
 *   Header: secret_key: {appSecret}
 *   Body (form): grant_type=app&amp;app_id={appId}
 *   Response: { access_token, refresh_token, expires_in (seconds) }
 * </pre>
 *
 * <p>Refresh flow:
 * <pre>
 *   POST https://oauth.zaloapp.com/v4/oa/access_token
 *   Header: secret_key: {appSecret}
 *   Body (form): grant_type=refresh_token&amp;app_id={appId}&amp;refresh_token={refreshToken}
 * </pre>
 *
 * <p>Tokens are stored encrypted in shop_config via {@link ShopConfigService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ZaloOaService {

    private static final String TOKEN_URL = "https://oauth.zaloapp.com/v4/oa/access_token";
    /**
     * Refresh 5 minutes before actual expiry so we never hand out a token that
     * expires mid-request.
     */
    private static final int EXPIRY_BUFFER_SECONDS = 300;

    private final ShopConfigService shopConfigService;
    private final RestTemplate restTemplate;
    private final MessageService messageService;

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Fetches a fresh access token from Zalo using the supplied credentials,
     * validates them, and persists everything encrypted in shop_config.
     *
     * @throws IllegalArgumentException if Zalo rejects the credentials or is unreachable
     */
    public ZaloOaStatusDTO connect(ConnectZaloOaRequest request) {
        String appId = request.getAppId().trim();
        String appSecret = request.getAppSecret().trim();

        TokenResponse token;
        try {
            token = fetchToken(appId, appSecret, "app", null);
        } catch (IllegalArgumentException e) {
            // Zalo responded with an error code — re-throw as-is (message already user-facing)
            throw e;
        } catch (RestClientException e) {
            // Network / timeout failure — surface as a clean 400 instead of a raw 500
            log.warn("Zalo OA connect failed — cannot reach Zalo API for appId={}: {}", appId, e.getMessage());
            throw new IllegalArgumentException(messageService.getMessage("error.zaloOa.authFailed"));
        }

        persist(appId, appSecret, token);
        shopConfigService.set(ShopConfigKey.ZALO_OA_NAME, request.getOaName().trim());
        shopConfigService.set(ShopConfigKey.ZALO_OA_ID,
                request.getOaId() != null ? request.getOaId().trim() : null);
        log.info("Zalo OA connected for tenant — appId={}, oaName={}", appId, request.getOaName());
        return buildStatus(appId, token.expiryAt());
    }

    /**
     * Clears all Zalo OA credentials for the current tenant.
     * After this, the tenant falls back to the platform's global ZNS token.
     */
    public void disconnect() {
        shopConfigService.set(ShopConfigKey.ZALO_APP_ID, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_APP_SECRET, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_OA_ACCESS_TOKEN, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_OA_REFRESH_TOKEN, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_OA_NAME, (String) null);
        shopConfigService.set(ShopConfigKey.ZALO_OA_ID, (String) null);
        log.info("Zalo OA disconnected");
    }

    /**
     * Returns the current connection status without leaking credentials.
     */
    public ZaloOaStatusDTO getStatus() {
        String appId = shopConfigService.getString(ShopConfigKey.ZALO_APP_ID);
        if (appId == null || appId.isBlank()) {
            return ZaloOaStatusDTO.builder().connected(false).build();
        }
        String expiryStr = shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY);
        String accessToken = shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN);
        LocalDateTime expiry = parseExpiry(expiryStr);
        boolean connected = accessToken != null && expiry != null && LocalDateTime.now().isBefore(expiry);
        return buildStatus(appId, connected ? expiry : null);
    }

    /**
     * Returns a valid access token for the current tenant's Zalo OA,
     * auto-refreshing if the stored token is near expiry.
     * Returns {@code null} when no credentials are configured — the caller
     * should then fall back to the platform's global access token.
     *
     * <p><strong>Must be called within a transaction with TenantContext set.</strong>
     */
    public String getAccessToken() {
        String appId = shopConfigService.getString(ShopConfigKey.ZALO_APP_ID);
        if (appId == null || appId.isBlank()) return null;

        String expiryStr = shopConfigService.getString(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY);
        LocalDateTime expiry = parseExpiry(expiryStr);
        String storedToken = shopConfigService.getString(ShopConfigKey.ZALO_OA_ACCESS_TOKEN);

        // Valid and not near expiry — return immediately
        if (storedToken != null && expiry != null
                && LocalDateTime.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(expiry)) {
            return storedToken;
        }

        // Attempt refresh
        String appSecret = shopConfigService.getString(ShopConfigKey.ZALO_APP_SECRET);
        if (appSecret == null) {
            log.warn("Zalo OA appSecret missing for tenant — cannot refresh token");
            return null;
        }

        String refreshToken = shopConfigService.getString(ShopConfigKey.ZALO_OA_REFRESH_TOKEN);
        try {
            TokenResponse token;
            if (refreshToken != null && !refreshToken.isBlank()) {
                token = fetchToken(appId, appSecret, "refresh_token", refreshToken);
            } else {
                token = fetchToken(appId, appSecret, "app", null);
            }
            persist(appId, appSecret, token);
            log.info("Zalo OA token refreshed for appId={}", appId);
            return token.accessToken();
        } catch (Exception e) {
            log.warn("Zalo OA token refresh failed for appId={}: {}", appId, e.getMessage());
            return null;
        }
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private TokenResponse fetchToken(String appId, String appSecret, String grantType, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("secret_key", appSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("app_id", appId);
        if ("refresh_token".equals(grantType) && refreshToken != null) {
            body.add("refresh_token", refreshToken);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

        if (response == null) {
            throw new IllegalArgumentException("Empty response from Zalo token endpoint");
        }

        Object error = response.get("error");
        if (error != null && !Integer.valueOf(0).equals(error)) {
            String msg = String.valueOf(response.getOrDefault("message", "Unknown error"));
            throw new IllegalArgumentException("Zalo OA authentication failed: " + msg);
        }

        String accessToken = (String) response.get("access_token");
        String newRefreshToken = (String) response.getOrDefault("refresh_token", refreshToken);
        int expiresIn = ((Number) response.getOrDefault("expires_in", 7776000)).intValue();
        LocalDateTime expiryAt = LocalDateTime.now().plusSeconds(expiresIn);

        return new TokenResponse(accessToken, newRefreshToken, expiryAt);
    }

    private void persist(String appId, String appSecret, TokenResponse token) {
        shopConfigService.set(ShopConfigKey.ZALO_APP_ID, appId);
        shopConfigService.set(ShopConfigKey.ZALO_APP_SECRET, appSecret);
        shopConfigService.set(ShopConfigKey.ZALO_OA_ACCESS_TOKEN, token.accessToken());
        shopConfigService.set(ShopConfigKey.ZALO_OA_REFRESH_TOKEN, token.refreshToken());
        shopConfigService.set(ShopConfigKey.ZALO_OA_TOKEN_EXPIRY, token.expiryAt().toString());
    }

    private ZaloOaStatusDTO buildStatus(String appId, LocalDateTime expiry) {
        return ZaloOaStatusDTO.builder()
                .connected(expiry != null)
                .appId(appId)
                .oaName(shopConfigService.getString(ShopConfigKey.ZALO_OA_NAME))
                .oaId(shopConfigService.getString(ShopConfigKey.ZALO_OA_ID))
                .tokenExpiry(expiry)
                .build();
    }

    private static LocalDateTime parseExpiry(String isoStr) {
        if (isoStr == null || isoStr.isBlank()) return null;
        try { return LocalDateTime.parse(isoStr); }
        catch (Exception e) { return null; }
    }

    private record TokenResponse(String accessToken, String refreshToken, LocalDateTime expiryAt) {}
}

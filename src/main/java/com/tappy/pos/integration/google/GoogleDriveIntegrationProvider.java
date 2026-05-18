package com.tappy.pos.integration.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.integration.IntegrationProvider;
import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;
import com.tappy.pos.model.entity.integration.IntegrationStatus;
import com.tappy.pos.model.entity.integration.ShopIntegration;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.integration.ShopIntegrationRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveIntegrationProvider implements IntegrationProvider {

    public static final String TYPE = "GOOGLE_DRIVE";

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPE =
            "https://www.googleapis.com/auth/drive.file " +
            "https://www.googleapis.com/auth/userinfo.email";

    @Value("${integration.google-drive.client-id:}")
    private String clientId;

    @Value("${integration.google-drive.redirect-uri:http://localhost:6868/api/integrations/oauth/callback}")
    private String redirectUri;

    private final OAuthStateStore           stateStore;
    private final GoogleDriveService        driveService;
    private final ShopIntegrationRepository integrationRepo;
    private final TenantContext             tenantContext;
    private final TenantRepository          tenantRepository;
    private final ObjectMapper              objectMapper;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String buildAuthUrl(String tenantId) {
        String nonce = stateStore.generate(tenantId);
        return UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("client_id",     clientId)
                .queryParam("redirect_uri",  redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope",         SCOPE)
                .queryParam("access_type",   "offline")
                .queryParam("prompt",        "consent")
                .queryParam("state",         nonce)
                .build().toUriString();
    }

    /**
     * NOT @Transactional — this method is called from the OAuth callback endpoint
     * which has no X-Tenant-ID header, so TenantContext is empty.
     * We set TenantContext manually here before any repository call so that
     * TenantRlsAspect (which fires at @Transactional repo boundaries) can
     * activate the correct RLS policy.
     */
    @Override
    public IntegrationStatusDTO handleCallback(String code, String state) {
        String tenantId = stateStore.consume(state);
        if (tenantId == null) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }

        // Network calls first — no DB, no transaction needed
        GoogleDriveService.TokenResult tokens = driveService.exchangeCode(code);
        String email    = driveService.getConnectedEmail(tokens.accessToken());
        String shopName = resolveShopName(tenantId);
        GoogleDriveService.FolderStructure folders =
                driveService.createShopFolders(tokens.accessToken(), shopName);

        GoogleDriveCredentials creds = GoogleDriveCredentials.builder()
                .refreshToken(tokens.refreshToken())
                .accessToken(tokens.accessToken())
                .tokenExpiresAt(tokens.expiresAt())
                .email(email)
                .rootFolderId(folders.rootFolderId())
                .folderIds(folders.folderIds())
                .build();

        // Set TenantContext so TenantRlsAspect activates RLS for every
        // repo call below (each repo call starts its own @Transactional).
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
        tenantContext.setCurrentTenant(tenant);
        try {
            ShopIntegration integration = integrationRepo
                    .findByTenantIdAndIntegrationTypeAndDeletedFalse(tenantId, TYPE)
                    .orElse(ShopIntegration.builder()
                            .tenantId(tenantId)
                            .integrationType(TYPE)
                            .build());

            integration.setConfigJson(toJson(creds));
            integration.setStatus(IntegrationStatus.CONNECTED);
            integration.setConnectedAt(LocalDateTime.now());
            integration.setDisconnectedAt(null);
            integrationRepo.save(integration);

            log.info("Google Drive connected for tenant={}, email={}", tenantId, email);

            return IntegrationStatusDTO.builder()
                    .integrationType(TYPE)
                    .status(IntegrationStatus.CONNECTED.name())
                    .email(email)
                    .connectedAt(integration.getConnectedAt())
                    .build();
        } finally {
            tenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationStatusDTO getStatus(String tenantId) {
        return integrationRepo
                .findByTenantIdAndIntegrationTypeAndDeletedFalse(tenantId, TYPE)
                .map(i -> {
                    GoogleDriveCredentials creds = fromJson(i.getConfigJson());
                    return IntegrationStatusDTO.builder()
                            .integrationType(TYPE)
                            .status(i.getStatus().name())
                            .email(creds != null ? creds.getEmail() : null)
                            .connectedAt(i.getConnectedAt())
                            .disconnectedAt(i.getDisconnectedAt())
                            .build();
                })
                .orElse(IntegrationStatusDTO.builder()
                        .integrationType(TYPE)
                        .status("NOT_CONFIGURED")
                        .build());
    }

    @Override
    @Transactional
    public void disconnect(String tenantId) {
        integrationRepo
                .findByTenantIdAndIntegrationTypeAndDeletedFalse(tenantId, TYPE)
                .ifPresent(i -> {
                    // Clear tokens but keep folder IDs so reconnecting the same
                    // account restores access to existing Drive files.
                    GoogleDriveCredentials creds = fromJson(i.getConfigJson());
                    if (creds != null) {
                        creds.setRefreshToken(null);
                        creds.setAccessToken(null);
                        creds.setTokenExpiresAt(0L);
                        i.setConfigJson(toJson(creds));
                    }
                    i.setStatus(IntegrationStatus.DISCONNECTED);
                    i.setDisconnectedAt(LocalDateTime.now());
                    integrationRepo.save(i);
                    log.info("Google Drive disconnected for tenant={}", tenantId);
                });
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns a valid GoogleDriveCredentials with a fresh access token.
     * Called by the upload service in Phase 2.
     */
    public GoogleDriveCredentials getCredentialsWithFreshToken(String tenantId) {
        ShopIntegration integration = integrationRepo
                .findByTenantIdAndIntegrationTypeAndDeletedFalse(tenantId, TYPE)
                .orElseThrow(() -> new IllegalStateException("Google Drive not configured"));

        if (integration.getStatus() != IntegrationStatus.CONNECTED) {
            throw new IllegalStateException("Google Drive is not connected");
        }

        GoogleDriveCredentials creds = fromJson(integration.getConfigJson());
        if (creds == null || creds.getRefreshToken() == null) {
            throw new IllegalStateException("Google Drive credentials are missing");
        }

        // Refresh token if needed and persist the new access token
        try {
            driveService.getValidAccessToken(creds);
        } catch (DriveTokenRevokedException e) {
            // Google revoked the refresh token — auto-disconnect so the user is prompted to reconnect
            creds.setRefreshToken(null);
            creds.setAccessToken(null);
            creds.setTokenExpiresAt(0L);
            integration.setConfigJson(toJson(creds));
            integration.setStatus(IntegrationStatus.DISCONNECTED);
            integration.setDisconnectedAt(LocalDateTime.now());
            integrationRepo.save(integration);
            log.warn("Google Drive refresh token revoked for tenant={}, marked as DISCONNECTED", tenantId);
            throw new IllegalStateException(
                    "Google Drive authorization has been revoked. Please reconnect in Shop Settings.");
        }
        integration.setConfigJson(toJson(creds));
        integrationRepo.save(integration);
        return creds;
    }

    /**
     * Resolves shop name for Drive folder naming.
     * Called before TenantContext is set, so shopInfoService is not usable here.
     * Falls back to tenantId which is always available.
     */
    private String resolveShopName(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(t -> t.getName() != null ? t.getName() : tenantId)
                .orElse(tenantId);
    }

    private String toJson(GoogleDriveCredentials creds) {
        try {
            return objectMapper.writeValueAsString(creds);
        } catch (Exception e) {
            log.error("Failed to serialize GoogleDriveCredentials", e);
            return null;
        }
    }

    private GoogleDriveCredentials fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, GoogleDriveCredentials.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize GoogleDriveCredentials: {}", e.getMessage());
            return null;
        }
    }
}

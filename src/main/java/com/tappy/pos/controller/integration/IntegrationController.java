package com.tappy.pos.controller.integration;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;
import com.tappy.pos.service.integration.IntegrationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    @Value("${app.frontend-url:http://localhost:4000}")
    private String frontendUrl;

    private final IntegrationService integrationService;
    private final TenantContext      tenantContext;

    /**
     * Returns the OAuth authorization URL so the frontend can open it in a popup.
     * Requires the GOOGLE_DRIVE feature (or SHOP_INFO as a fallback).
     */
    @GetMapping("/{type}/auth-url")
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl(
            @PathVariable String type,
            @RequestParam(required = false) String origin) {
        String tenantId = tenantContext.getCurrentTenantId();
        // Only carry the origin through OAuth state if it's one of ours — guards
        // against an open redirect on the callback.
        String safeOrigin = isAllowedOrigin(origin) ? origin : null;
        String url = integrationService.getAuthUrl(type.toUpperCase(), tenantId, safeOrigin);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    /**
     * OAuth2 callback endpoint — called by Google after the user grants access.
     * Not behind @RequiresFeature because Google calls this without our headers.
     * Validates the state nonce (CSRF protection) and redirects to the frontend
     * callback page via a popup-friendly redirect.
     */
    @GetMapping("/oauth/callback")
    public void oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, defaultValue = "GOOGLE_DRIVE") String type,
            HttpServletResponse response) throws IOException {

        // Redirect back to the host the flow started on (e.g. the tenant subdomain)
        // so the popup's postMessage to its opener is same-origin. Peeked from the
        // state without consuming it; falls back to the configured apex URL.
        String redirectBase = resolveRedirectBase(type, state);

        if (error != null || code == null || state == null) {
            log.warn("OAuth callback with error={}, code={}", error, code != null ? "present" : "null");
            response.sendRedirect(redirectBase + "/oauth-callback?status=error&type=" + type.toUpperCase()
                    + "&reason=" + (error != null ? error : "missing_params"));
            return;
        }

        try {
            integrationService.handleOAuthCallback(code, state, type.toUpperCase());
            response.sendRedirect(redirectBase + "/oauth-callback?status=success&type=" + type.toUpperCase());
        } catch (Exception e) {
            log.error("OAuth callback failed for type={}: {}", type, e.getMessage());
            response.sendRedirect(redirectBase + "/oauth-callback?status=error&type=" + type.toUpperCase()
                    + "&reason=server_error");
        }
    }

    /** The validated frontend origin for the state nonce, or the configured apex URL. */
    private String resolveRedirectBase(String type, String state) {
        if (state == null) return frontendUrl;
        try {
            String origin = integrationService.peekOAuthOrigin(type.toUpperCase(), state);
            return isAllowedOrigin(origin) ? origin : frontendUrl;
        } catch (Exception e) {
            return frontendUrl;
        }
    }

    /**
     * True only for our own hosts (pos.tappy.vn + its subdomains, localhost) — open-redirect guard.
     * tappypos.vn is kept during the redirect-coexistence window; remove after cutover is verified.
     */
    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) return false;
        try {
            String host = java.net.URI.create(origin).getHost();
            return host != null && (
                    host.equals("pos.tappy.vn") || host.endsWith(".pos.tappy.vn")
                    || host.equals("tappypos.vn") || host.endsWith(".tappypos.vn")
                    || host.equals("localhost"));
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns connection status for one integration type. */
    @GetMapping("/{type}/status")
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<IntegrationStatusDTO>> getStatus(
            @PathVariable String type) {
        String tenantId = tenantContext.getCurrentTenantId();
        IntegrationStatusDTO status = integrationService.getStatus(type.toUpperCase(), tenantId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /** Disconnects an integration. Warns the user via the frontend; data is preserved. */
    @DeleteMapping("/{type}")
    @RequiresFeature("SHOP_INFO")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable String type) {
        String tenantId = tenantContext.getCurrentTenantId();
        integrationService.disconnect(type.toUpperCase(), tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

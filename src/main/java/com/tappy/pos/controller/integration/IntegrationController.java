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
            @PathVariable String type) {
        String tenantId = tenantContext.getCurrentTenantId();
        String url      = integrationService.getAuthUrl(type.toUpperCase(), tenantId);
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

        if (error != null || code == null || state == null) {
            log.warn("OAuth callback with error={}, code={}", error, code != null ? "present" : "null");
            response.sendRedirect(frontendUrl + "/oauth-callback?status=error&type=" + type.toUpperCase()
                    + "&reason=" + (error != null ? error : "missing_params"));
            return;
        }

        try {
            IntegrationStatusDTO result = integrationService.handleOAuthCallback(code, state, type.toUpperCase());
            String tenantId = result.getEmail() != null ? extractTenantFromState(state) : "unknown";
            response.sendRedirect(frontendUrl + "/oauth-callback?status=success&type=" + type.toUpperCase());
        } catch (Exception e) {
            log.error("OAuth callback failed for type={}: {}", type, e.getMessage());
            response.sendRedirect(frontendUrl + "/oauth-callback?status=error&type=" + type.toUpperCase()
                    + "&reason=server_error");
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

    private String extractTenantFromState(String state) {
        // State is a nonce that was already consumed in handleCallback;
        // tenantId was embedded during callback processing — this is just for logging.
        return "resolved-in-provider";
    }
}

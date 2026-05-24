package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.GenerateInvitationRequest;
import com.tappy.pos.model.dto.tenant.InvitationCodeResponse;
import com.tappy.pos.model.dto.tenant.InvitationPreviewResponse;
import com.tappy.pos.model.dto.tenant.JoinShopRequest;
import com.tappy.pos.service.tenant.ShopInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mobile endpoints for the shop-invitation feature.
 *
 * Tenant-scoped (requires X-Tenant-ID header + JWT):
 *   POST /shop-config/invitations   — shop owner generates a code (requires USER feature)
 *
 * Master-scoped (requires JWT only, no tenant context):
 *   GET  /invitations/preview       — invitee looks up a code before confirming
 *   POST /invitations/join          — invitee accepts the invitation
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ShopInvitationController {

    private final ShopInvitationService invitationService;

    // ── Shop owner: generate invitation code (tenant-scoped) ──────────────────

    /**
     * Shop owner generates a 6-character invitation code valid for 5 minutes.
     * POST /api/v1/shop-config/invitations
     *
     * Requires: JWT with USER feature (SHOP_OWNER and MANAGER have this by default).
     */
    @PostMapping("/shop-config/invitations")
    @RequiresFeature("USER")
    public ResponseEntity<ApiResponse<InvitationCodeResponse>> generateInvitation(
            @RequestBody @Valid GenerateInvitationRequest request) {
        log.info("POST /shop-config/invitations - role: {}", request.getRoleName());
        InvitationCodeResponse response = invitationService.generate(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Mã mời đã được tạo thành công"));
    }

    // ── Invitee: preview code (master-scoped, any authenticated user) ─────────

    /**
     * Preview shop info for an invitation code without accepting it.
     * GET /api/v1/invitations/preview?code=A7XK3Q
     *
     * Requires: any valid JWT (the user must be logged in but not necessarily in a shop).
     * Returns 404 if the code is not found or has expired/been used.
     */
    @GetMapping("/invitations/preview")
    public ResponseEntity<ApiResponse<InvitationPreviewResponse>> previewInvitation(
            @RequestParam String code) {
        log.info("GET /invitations/preview - code: {}", code);
        InvitationPreviewResponse preview = invitationService.preview(code);
        return ResponseEntity.ok(ApiResponse.success(preview, "OK"));
    }

    // ── Invitee: accept invitation (master-scoped, any authenticated user) ────

    /**
     * Accept an invitation — joins the authenticated user to the shop.
     * POST /api/v1/invitations/join
     *
     * Requires: any valid JWT (user must not already belong to a tenant).
     * Returns a new accessToken for the joined shop so the mobile can navigate
     * directly without requiring a re-login.
     */
    @PostMapping("/invitations/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinShop(
            @RequestBody @Valid JoinShopRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("POST /invitations/join - code: {}, user: {}", request.getCode(), username);
        Map<String, Object> result = invitationService.join(request.getCode(), username);
        return ResponseEntity.ok(ApiResponse.success(result, "Bạn đã tham gia cửa hàng thành công"));
    }
}

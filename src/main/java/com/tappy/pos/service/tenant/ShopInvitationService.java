package com.tappy.pos.service.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.config.JwtTokenProvider;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.GenerateInvitationRequest;
import com.tappy.pos.model.dto.tenant.InvitationCodeResponse;
import com.tappy.pos.model.dto.tenant.InvitationPreviewResponse;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.tenant.ShopInvitation;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.ShopInvitationRepository;
import com.tappy.pos.service.auth.RoleFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopInvitationService {

    // ── code alphabet: uppercase alphanumeric, no ambiguous chars (0/O, 1/I/L) ──
    private static final String ALPHABET   = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int    CODE_LEN   = 6;
    private static final int    TTL_MINUTES = 5;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ShopInvitationRepository invitationRepo;
    private final RoleRepository           roleRepository;
    private final UserRepository           userRepository;
    private final TenantService            tenantService;
    private final RoleFeatureService       roleFeatureService;
    private final TenantFeatureService     tenantFeatureService;
    private final TenantProvisioningService tenantProvisioningService;
    private final JwtTokenProvider         jwtTokenProvider;
    private final ObjectMapper             objectMapper;
    private final TenantContext            tenantContext;
    private final SecureRandom             secureRandom = new SecureRandom();

    // ── generate ───────────────────────────────────────────────────────────────

    /**
     * Generate an invitation code for the current tenant.
     * Called by a shop owner; requires tenant context to already be set.
     *
     * @param request contains roleName + optional features override
     * @return the generated code with expiry
     */
    @Transactional
    public InvitationCodeResponse generate(GenerateInvitationRequest request) {
        String tenantId  = tenantContext.getCurrentTenantId();
        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();

        // Validate role exists for this tenant
        String roleName = request.getRoleName();
        if (!roleRepository.existsByNameAndTenantId(roleName, tenantId)) {
            throw new BadRequestException("Role '" + roleName + "' does not exist for this shop");
        }

        // Determine feature list: use request override or fall back to role defaults
        List<String> features = (request.getFeatures() != null && !request.getFeatures().isEmpty())
                ? request.getFeatures()
                : roleFeatureService.getActiveFeatureNamesByRoleName(roleName);

        String code = generateUniqueCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TTL_MINUTES);

        ShopInvitation invitation = ShopInvitation.builder()
                .tenantId(tenantId)
                .code(code)
                .roleName(roleName)
                .features(toJson(features))
                .createdBy(createdBy)
                .expiresAt(expiresAt)
                .build();

        invitationRepo.save(invitation);
        log.info("Generated invitation code '{}' for tenant={}, role={}, by={}",
                code, tenantId, roleName, createdBy);

        return new InvitationCodeResponse(code, roleName, features,
                expiresAt.format(ISO), invitation.secondsRemaining());
    }

    // ── preview ────────────────────────────────────────────────────────────────

    /**
     * Look up an invitation code and return shop info — without marking it used.
     * Called before the invitee confirms, so they can see which shop they're joining.
     */
    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String code) {
        ShopInvitation invitation = findValidOrThrow(code);
        Tenant tenant = tenantService.getTenantEntity(invitation.getTenantId());
        return new InvitationPreviewResponse(
                tenant.getName(),
                tenant.getShopType() != null ? tenant.getShopType().name() : "OTHER",
                invitation.getRoleName(),
                invitation.getExpiresAt().format(ISO),
                invitation.secondsRemaining()
        );
    }

    // ── join ───────────────────────────────────────────────────────────────────

    /**
     * Accept an invitation — joins the current authenticated user to the shop.
     *
     * Rules:
     *  - User must not already belong to a tenant (tenant_id IS NULL).
     *  - Code must be valid (unused + not expired).
     *  - Returns a fresh JWT for the new tenant context so the mobile can
     *    immediately navigate into the app without re-logging-in.
     *
     * @return map containing {@code accessToken} and {@code tenantId}
     */
    @Transactional
    public Map<String, Object> join(String code, String username) {
        ShopInvitation invitation = findValidOrThrow(code);

        // Load user — must be pre-provision (no tenant yet)
        User user = userRepository.findByUsernameAndNullTenant(username)
                .orElseThrow(() -> new BadRequestException(
                        "Bạn đang là thành viên của một cửa hàng khác. Vui lòng liên hệ hỗ trợ."));

        String tenantId = invitation.getTenantId();
        Tenant tenant   = tenantService.getTenantEntity(tenantId);

        // Set tenant context so JPA/RLS writes go to the right tenant
        tenantContext.setCurrentTenant(tenant);
        try {
            // Assign role to user
            Role role = roleRepository.findByNameAndTenantId(invitation.getRoleName(), tenantId)
                    .orElseThrow(() -> new BadRequestException(
                            "Role '" + invitation.getRoleName() + "' not found in shop"));

            user.setTenantId(tenantId);
            user.getRoles().add(role);
            userRepository.save(user);

            // Mark invitation as used
            invitation.setUsedAt(LocalDateTime.now());
            invitation.setUsedBy(username);
            invitationRepo.save(invitation);

            // Issue a new JWT for this tenant
            List<String> roleNames   = List.of(invitation.getRoleName());
            List<String> featureNames = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

            String accessToken = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                    username, roleNames, featureNames, false,
                    tenant.getShopType() != null ? tenant.getShopType().name() : null,
                    tenantId);

            log.info("User '{}' joined tenant '{}' as role '{}' via invitation code '{}'",
                    username, tenantId, invitation.getRoleName(), code);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accessToken", accessToken);
            result.put("tenantId", tenantId);
            return result;

        } finally {
            tenantContext.clear();
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private ShopInvitation findValidOrThrow(String code) {
        return invitationRepo.findValidByCode(code.toUpperCase().trim(), LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mã mời không hợp lệ hoặc đã hết hạn."));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LEN);
            for (int i = 0; i < CODE_LEN; i++) {
                sb.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (!invitationRepo.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique invitation code — please retry");
    }

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return List.of(); }
    }
}

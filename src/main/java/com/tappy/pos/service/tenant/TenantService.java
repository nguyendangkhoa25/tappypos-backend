package com.tappy.pos.service.tenant;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.ForbiddenException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.tenant.CreateTenantRequest;
import com.tappy.pos.model.dto.tenant.TenantDTO;
import com.tappy.pos.model.dto.tenant.TenantStatsDTO;
import com.tappy.pos.model.dto.tenant.TenantStatusResponse;
import com.tappy.pos.model.dto.tenant.UpdateTenantRequest;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final AuthContext authContext;

    private String deriveTenantDbName(String tenantId) {
        return tenantId;
    }

    private String getCurrentUsername() {
        String username = authContext.getCurrentUsername();
        return username != null ? username : "system";
    }

    /**
     * Returns true when the authenticated user holds the AGENT role.
     * Used to decide whether to scope queries to a single agent.
     */
    private boolean isAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "AGENT".equals(a.getAuthority()));
    }

    /**
     * Resolves the Agent row for the current user.
     * Returns empty when the user is master admin (has no agent row).
     */
    private Optional<Agent> getCurrentAgent() {
        String username = getCurrentUsername();
        return userRepository.findByUsernameTenantScoped(username)
                .flatMap(u -> agentRepository.findByUserId(u.getId()));
    }

    /**
     * Throws ForbiddenException if the current user is an AGENT who does not
     * own the given tenant. Master admins always pass.
     */
    private void assertVendorOwns(Tenant tenant) {
        if (!isAgent()) return;
        Agent agent = getCurrentAgent()
                .orElseThrow(() -> new ForbiddenException("error.access.vendor_not_found"));
        if (!agent.getId().equals(tenant.getVendorId())) {
            throw new ForbiddenException("error.access.not_your_shop");
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Public endpoint for mobile app — returns shop existence and activation status
     * without exposing full tenant details. Never throws; always returns 200.
     */
    public TenantStatusResponse checkStatus(String shopId) {
        Optional<Tenant> tenantOpt = tenantRepository.findByTenantId(shopId);
        if (tenantOpt.isEmpty()) {
            return new TenantStatusResponse(shopId, null, "NOT_FOUND");
        }
        Tenant tenant = tenantOpt.get();
        String status = tenant.isActive() ? "ACTIVE" : "SUSPENDED";
        return new TenantStatusResponse(shopId, tenant.getName(), status);
    }

    public List<TenantDTO> getAllActiveTenants() {
        return tenantRepository.findAllByActiveTrue()
                .stream().map(t -> mapToDTO(t, null)).collect(Collectors.toList());
    }

    /**
     * Returns all tenants visible to the caller.
     * AGENT sees only their own shops; master admin sees all.
     */
    public List<TenantDTO> getAllTenants(String search) {
        List<Tenant> tenants = resolveVisibleTenants();
        Map<Long, Agent> vendorCache = buildAgentCache(tenants);

        String term = (search != null) ? search.trim().toLowerCase() : null;
        return tenants.stream()
                .filter(t -> term == null || matchesSearch(t, term))
                .map(t -> mapToDTO(t, t.getVendorId() != null ? vendorCache.get(t.getVendorId()) : null))
                .collect(Collectors.toList());
    }

    /**
     * Dashboard stats scoped to the caller's visible tenants.
     */
    public TenantStatsDTO getStats() {
        List<Tenant> all = resolveVisibleTenants();
        LocalDate today = LocalDate.now();
        LocalDate soonThreshold = today.plusDays(7);

        long total       = all.size();
        long inactive    = all.stream().filter(t -> !t.isActive()).count();
        long expired     = all.stream()
                .filter(t -> t.isActive()
                        && t.getExpirationDate() != null
                        && t.getExpirationDate().isBefore(today))
                .count();
        long expiringSoon = all.stream()
                .filter(t -> t.isActive()
                        && t.getExpirationDate() != null
                        && !t.getExpirationDate().isBefore(today)
                        && !t.getExpirationDate().isAfter(soonThreshold))
                .count();
        long active = total - inactive - expired;

        return new TenantStatsDTO(total, active, inactive, expiringSoon, expired);
    }

    public TenantDTO getTenantById(String tenantId) {
        Tenant tenant = findTenant(tenantId);
        assertVendorOwns(tenant);
        Agent agent = tenant.getVendorId() != null
                ? agentRepository.findById(tenant.getVendorId()).orElse(null) : null;
        return mapToDTO(tenant, agent);
    }

    public TenantDTO createTenant(CreateTenantRequest request) {
        if (tenantRepository.findByTenantId(request.getTenantId()).isPresent()) {
            throw new RuntimeException("Tenant already exists: " + request.getTenantId());
        }

        String currentUser = getCurrentUsername();
        String dbName = (request.getDbName() != null && !request.getDbName().isBlank())
                ? request.getDbName() : deriveTenantDbName(request.getTenantId());

        Long vendorId = resolveVendorIdForCreate(request);

        Tenant tenant = Tenant.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .dbName(dbName)
                .active(true)
                .expirationDate(request.getExpirationDate())
                .maxUsers(request.getMaxUsers())
                .features(request.getFeatures() != null ? String.join(",", request.getFeatures()) : null)
                .subscriptionType(request.getSubscriptionType())
                .shopType(request.getShopType())
                .contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone())
                .contactPersonEmail(request.getContactPersonEmail())
                .contactPersonZaloId(request.getContactPersonZaloId())
                .vendorId(vendorId)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .activeAt(System.currentTimeMillis())
                .activeBy(currentUser)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created: {} by {} (vendorId={})", saved.getTenantId(), currentUser, vendorId);

        Agent agent = vendorId != null
                ? agentRepository.findById(vendorId).orElse(null) : null;
        return mapToDTO(saved, agent);
    }

    public TenantDTO updateTenant(String tenantId, UpdateTenantRequest request) {
        Tenant tenant = findTenant(tenantId);
        assertVendorOwns(tenant);

        String currentUser = getCurrentUsername();

        if (request.getName()              != null) tenant.setName(request.getName());
        if (request.getExpirationDate()    != null) tenant.setExpirationDate(request.getExpirationDate());
        if (request.getMaxUsers()          != null) tenant.setMaxUsers(request.getMaxUsers());
        if (request.getSubscriptionType()  != null) tenant.setSubscriptionType(request.getSubscriptionType());
        if (request.getContactPersonName() != null) tenant.setContactPersonName(request.getContactPersonName());
        if (request.getContactPersonPhone()!= null) tenant.setContactPersonPhone(request.getContactPersonPhone());
        if (request.getContactPersonEmail()!= null) tenant.setContactPersonEmail(request.getContactPersonEmail());
        if (request.getContactPersonZaloId()!=null) tenant.setContactPersonZaloId(request.getContactPersonZaloId());

        // Only master admin may change features, dbName, or reassign vendor
        if (!isAgent()) {
            if (request.getFeatures() != null) tenant.setFeatures(String.join(",", request.getFeatures()));
            if (request.getDbName()   != null) tenant.setDbName(request.getDbName());
            if (request.getVendorId() != null) tenant.setVendorId(request.getVendorId());
        }

        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant updated: {} by {}", tenantId, currentUser);

        Agent agent = updated.getVendorId() != null
                ? agentRepository.findById(updated.getVendorId()).orElse(null) : null;
        return mapToDTO(updated, agent);
    }

    /**
     * Only master admin can permanently delete a shop.
     */
    public void deleteTenant(String tenantId) {
        if (isAgent()) {
            throw new ForbiddenException("error.access.master_only");
        }
        Tenant tenant = findTenant(tenantId);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted: {}", tenantId);
    }

    public TenantDTO deactivateTenant(String tenantId) {
        Tenant tenant = findTenant(tenantId);
        assertVendorOwns(tenant);

        String currentUser = getCurrentUsername();
        tenant.setActive(false);
        tenant.setActiveAt(System.currentTimeMillis());
        tenant.setActiveBy(currentUser);
        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant deactivated: {} by {}", tenantId, currentUser);

        Agent agent = updated.getVendorId() != null
                ? agentRepository.findById(updated.getVendorId()).orElse(null) : null;
        return mapToDTO(updated, agent);
    }

    public TenantDTO activateTenant(String tenantId) {
        Tenant tenant = findTenant(tenantId);
        assertVendorOwns(tenant);

        String currentUser = getCurrentUsername();
        tenant.setActive(true);
        tenant.setActiveAt(System.currentTimeMillis());
        tenant.setActiveBy(currentUser);
        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant activated: {} by {}", tenantId, currentUser);

        Agent agent = updated.getVendorId() != null
                ? agentRepository.findById(updated.getVendorId()).orElse(null) : null;
        return mapToDTO(updated, agent);
    }

    public Tenant getTenantEntity(String tenantId) {
        return findTenant(tenantId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Tenant findTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }

    /**
     * Returns the list of tenants the caller may see:
     * - AGENT → only tenants assigned to their agent
     * - master admin → all tenants
     */
    private List<Tenant> resolveVisibleTenants() {
        if (isAgent()) {
            return getCurrentAgent()
                    .map(a -> tenantRepository.findAllByVendorId(a.getId()))
                    .orElse(List.of());
        }
        return tenantRepository.findAll();
    }

    /**
     * For creates: if AGENT, ignore the request's vendorId and use their own.
     * If master admin, use the request's vendorId (may be null).
     */
    private Long resolveVendorIdForCreate(CreateTenantRequest request) {
        if (isAgent()) {
            return getCurrentAgent()
                    .map(Agent::getId)
                    .orElseThrow(() -> new ForbiddenException("error.access.vendor_not_found"));
        }
        return request.getVendorId();
    }

    private boolean matchesSearch(Tenant t, String term) {
        return (t.getName() != null && t.getName().toLowerCase().contains(term))
                || (t.getDbName() != null && t.getDbName().toLowerCase().contains(term))
                || (t.getContactPersonName() != null && t.getContactPersonName().toLowerCase().contains(term))
                || (t.getContactPersonPhone() != null && t.getContactPersonPhone().toLowerCase().contains(term));
    }

    /**
     * Builds an id→Agent lookup map for a list of tenants
     * so we can attach agent names without N+1 queries.
     */
    private Map<Long, Agent> buildAgentCache(List<Tenant> tenants) {
        List<Long> vendorIds = tenants.stream()
                .map(Tenant::getVendorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (vendorIds.isEmpty()) return Map.of();
        return agentRepository.findAllById(vendorIds).stream()
                .collect(Collectors.toMap(Agent::getId, Function.identity()));
    }

    private TenantDTO mapToDTO(Tenant tenant, Agent agent) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .dbName(tenant.getDbName())
                .active(tenant.isActive())
                .expirationDate(tenant.getExpirationDate())
                .maxUsers(tenant.getMaxUsers())
                .features(tenant.getFeatures() != null
                        ? Arrays.asList(tenant.getFeatures().split(",")) : null)
                .subscriptionType(tenant.getSubscriptionType())
                .shopType(tenant.getShopType())
                .contactPersonName(tenant.getContactPersonName())
                .contactPersonPhone(tenant.getContactPersonPhone())
                .contactPersonEmail(tenant.getContactPersonEmail())
                .contactPersonZaloId(tenant.getContactPersonZaloId())
                .vendorId(tenant.getVendorId())
                .vendorName(agent != null ? agent.getName() : null)
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .activeAt(tenant.getActiveAt())
                .activeBy(tenant.getActiveBy())
                .createdBy(tenant.getCreatedBy())
                .updatedBy(tenant.getUpdatedBy())
                .build();
    }
}

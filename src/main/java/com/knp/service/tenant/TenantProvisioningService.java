package com.knp.service.tenant;

import com.knp.model.dto.tenant.RoleSetupRequest;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.entity.auth.User;
import com.knp.model.enums.RoleEnum;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.auth.RoleRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.repository.auth.UserRepository;
import com.knp.service.auth.RoleFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds roles, features, role-feature mappings, shop info, and admin user
 * into a newly provisioned tenant database.
 *
 * Must be called with TenantContext already set to the target tenant so that
 * all JPA operations are routed to the correct tenant datasource.
 * Walk-in customer is seeded by the shop-type DML script, not here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final RoleRepository roleRepository;
    private final RoleFeatureService roleFeatureService;
    private final UserRepository userRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final ShopConfigService shopConfigService;
    private final PasswordEncoder passwordEncoder;

    // Role → list of feature keys the role receives by default
    private static final Map<String, List<String>> ROLE_FEATURES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(RoleEnum.SHOP_OWNER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "SALARY", "CUSTOMER", "LOYALTY", "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "ACTIVITY_LOG", "PAWN", "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.MANAGER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "CUSTOMER", "LOYALTY", "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "ACTIVITY_LOG", "PAWN", "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.CASHIER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "POS", "CUSTOMER", "PROMOTION",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.ACCOUNTANT.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "REVENUE", "EXPENSE", "SALARY", "INVOICE", "ACCOUNTING", "CUSTOMER",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.WAREHOUSE_STAFF.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "INVENTORY", "PRODUCT", "VENDOR",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.PAWN_OFFICER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "PAWN", "CUSTOMER", "ORDER", "PRODUCT",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.SERVICE_STAFF.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "POS", "CUSTOMER",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.TECHNICIAN.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "PRODUCT", "CUSTOMER", "INVENTORY", "POS", "PAWN",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.RECEPTIONIST.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "CUSTOMER", "POS",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.CLEANER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "NOTIFICATION"
        ));
        ROLE_FEATURES = m;
    }

    public void provision(Tenant tenant, String adminUsername, String adminPassword,
                          List<RoleSetupRequest> roleSetups, String shopAddress) {
        log.info("Provisioning default data for tenant: {}", tenant.getTenantId());

        String tenantId = tenant.getTenantId();
        Map<String, List<String>> effectiveRoleFeatures = buildEffectiveRoleFeatures(roleSetups);

        try { seedRoles(effectiveRoleFeatures.keySet().stream().toList(), tenantId); }
        catch (Exception e) { log.warn("seedRoles failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { seedRoleFeatureMappings(effectiveRoleFeatures); }
        catch (Exception e) { log.warn("seedRoleFeatureMappings failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { seedShopInfo(tenant, shopAddress, tenantId); }
        catch (Exception e) { log.warn("seedShopInfo failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { seedDefaultConfig(); }
        catch (Exception e) { log.warn("seedDefaultConfig failed for tenant {}: {}", tenantId, e.getMessage()); }

        // Admin user must succeed — propagate failures to the caller.
        seedShopOwnerUser(tenant, adminUsername, adminPassword, tenantId);

        log.info("Provisioning complete for tenant: {}", tenantId);
    }

    /**
     * Builds the effective role→features map from provided roleSetups, falling back to
     * ROLE_FEATURES defaults for any missing roles. SHOP_OWNER is always included.
     */
    private Map<String, List<String>> buildEffectiveRoleFeatures(List<RoleSetupRequest> roleSetups) {
        if (roleSetups == null || roleSetups.isEmpty()) {
            return ROLE_FEATURES;
        }
        Map<String, List<String>> effective = new LinkedHashMap<>();
        for (RoleSetupRequest setup : roleSetups) {
            String roleName = setup.getRoleName();
            if (roleName == null || roleName.isBlank()) continue;
            // Skip master-only roles
            if (RoleEnum.MASTER_TENANT.getCode().equals(roleName) ||
                    RoleEnum.AGENT.getCode().equals(roleName)) continue;
            List<String> features = setup.getFeatures() != null ? setup.getFeatures()
                    : ROLE_FEATURES.getOrDefault(roleName, List.of());
            effective.put(roleName, features);
        }
        // Always ensure SHOP_OWNER is present
        effective.computeIfAbsent(RoleEnum.SHOP_OWNER.getCode(),
                k -> ROLE_FEATURES.getOrDefault(k, List.of()));
        return effective;
    }

    private void seedRoles(List<String> roleNames, String tenantId) {
        for (String roleName : roleNames) {
            RoleEnum roleEnum;
            try { roleEnum = RoleEnum.valueOf(roleName); }
            catch (IllegalArgumentException e) { continue; }
            if (!roleRepository.existsByName(roleEnum.getCode())) {
                Role role = new Role(roleEnum.getCode(), roleEnum.getDescription());
                role.setTenantId(tenantId);
                roleRepository.save(role);
                log.debug("Seeded role: {}", roleEnum.getCode());
            }
        }
    }

    private void seedRoleFeatureMappings(Map<String, List<String>> roleFeatures) {
        roleFeatures.forEach((roleName, features) -> {
            try {
                // Call through RoleFeatureService so TenantRlsAspect fires on the
                // service's @Transactional boundary and sets app.current_tenant before
                // the repository native query executes.
                roleFeatureService.setRoleFeatures(roleName, features);
            } catch (Exception e) {
                log.warn("Could not set features for role {}: {}", roleName, e.getMessage());
            }
        });
    }

    private void seedShopInfo(Tenant tenant, String shopAddress, String tenantId) {
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElse(ShopInfo.builder().tenantId(tenantId).build());
        shopInfo.setShopName(tenant.getName());
        shopInfo.setPhone(tenant.getContactPersonPhone());
        shopInfo.setEmail(tenant.getContactPersonEmail());
        if (shopAddress != null && !shopAddress.isBlank()) {
            shopInfo.setAddress(shopAddress);
        }
        shopInfoRepository.save(shopInfo);
        log.debug("Seeded shop_info for tenant: {}", tenantId);
    }

    private void seedDefaultConfig() {
        shopConfigService.seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.0);
        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, "STANDARD");
        log.debug("Seeded default shop_config");
    }

    private void seedShopOwnerUser(Tenant tenant, String adminUsername, String adminPassword, String tenantId) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("Admin user '{}' already exists in tenant: {}", adminUsername, tenantId);
            return;
        }

        Role shopOwnerRole = roleRepository.findByName(RoleEnum.SHOP_OWNER.getCode())
                .orElseThrow(() -> new RuntimeException("SHOP_OWNER role not found after seeding"));

        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .fullName(tenant.getContactPersonName() != null ? tenant.getContactPersonName() : "Admin")
                .email(tenant.getContactPersonEmail())
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction(null)
                .lang("vi")
                .build();
        admin.setTenantId(tenantId);
        admin.getRoles().add(shopOwnerRole);
        userRepository.save(admin);
        log.info("Created admin user '{}' for tenant: {}", adminUsername, tenantId);
    }
}

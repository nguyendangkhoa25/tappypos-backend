package com.knp.service.tenant;

import com.knp.model.dto.tenant.RoleSetupRequest;
import com.knp.model.entity.auth.Feature;
import com.knp.model.entity.auth.Role;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.entity.auth.User;
import com.knp.model.enums.FeatureEnum;
import com.knp.model.enums.RoleEnum;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.auth.RoleFeatureRepository;
import com.knp.repository.auth.RoleRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final RoleFeatureRepository roleFeatureRepository;
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

        Set<String> allowedFeatures = parseFeatures(tenant.getFeatures());
        Map<String, List<String>> effectiveRoleFeatures = buildEffectiveRoleFeatures(roleSetups);

        try { seedRoles(effectiveRoleFeatures.keySet().stream().toList()); }
        catch (Exception e) { log.warn("seedRoles failed for tenant {}: {}", tenant.getTenantId(), e.getMessage()); }

        try { seedFeatures(allowedFeatures); }
        catch (Exception e) { log.warn("seedFeatures failed for tenant {}: {}", tenant.getTenantId(), e.getMessage()); }

        try { seedRoleFeatureMappings(effectiveRoleFeatures); }
        catch (Exception e) { log.warn("seedRoleFeatureMappings failed for tenant {}: {}", tenant.getTenantId(), e.getMessage()); }

        try { seedShopInfo(tenant, shopAddress); }
        catch (Exception e) { log.warn("seedShopInfo failed for tenant {}: {}", tenant.getTenantId(), e.getMessage()); }

        //TODO later will have default config base on shop type at the create shop steps
        try { seedDefaultConfig(); }
        catch (Exception e) { log.warn("seedDefaultConfig failed for tenant {}: {}", tenant.getTenantId(), e.getMessage()); }

        // Admin user must succeed — propagate failures to the caller.
        seedShopOwnerUser(tenant, adminUsername, adminPassword);

        log.info("Provisioning complete for tenant: {}", tenant.getTenantId());
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
                    RoleEnum.VENDOR_ADMIN.getCode().equals(roleName)) continue;
            List<String> features = setup.getFeatures() != null ? setup.getFeatures()
                    : ROLE_FEATURES.getOrDefault(roleName, List.of());
            effective.put(roleName, features);
        }
        // Always ensure SHOP_OWNER is present
        effective.computeIfAbsent(RoleEnum.SHOP_OWNER.getCode(),
                k -> ROLE_FEATURES.getOrDefault(k, List.of()));
        return effective;
    }

    private void seedRoles(List<String> roleNames) {
        for (String roleName : roleNames) {
            RoleEnum roleEnum;
            try { roleEnum = RoleEnum.valueOf(roleName); }
            catch (IllegalArgumentException e) { continue; }
            if (!roleRepository.existsByName(roleEnum.getCode())) {
                roleRepository.save(new Role(roleEnum.getCode(), roleEnum.getDescription()));
                log.debug("Seeded role: {}", roleEnum.getCode());
            }
        }
    }

    private void seedFeatures(Set<String> allowedFeatures) {
        for (FeatureEnum featureEnum : FeatureEnum.values()) {
            if (!allowedFeatures.contains(featureEnum.getKey())) continue;
            boolean exists = roleFeatureRepository.findAll().stream()
                    .anyMatch(f -> featureEnum.getKey().equals(f.getName()));
            if (!exists) {
                roleFeatureRepository.save(new Feature(
                        featureEnum.getKey(),
                        featureEnum.getDisplayName(),
                        featureEnum.getDescription()
                ));
                log.debug("Seeded feature: {}", featureEnum.getKey());
            }
        }
    }

    private Set<String> parseFeatures(String featuresStr) {
        if (featuresStr == null || featuresStr.isBlank()) return Set.of();
        return Arrays.stream(featuresStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void seedRoleFeatureMappings(Map<String, List<String>> roleFeatures) {
        roleFeatures.forEach((roleName, features) ->
            features.forEach(featureName -> {
                try {
                    roleFeatureRepository.assignFeatureToRole(roleName, featureName);
                } catch (Exception e) {
                    log.warn("Could not assign feature {} to role {}: {}", featureName, roleName, e.getMessage());
                }
            })
        );
    }

    private void seedShopInfo(Tenant tenant, String shopAddress) {
        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElse(ShopInfo.builder().build());
        shopInfo.setShopName(tenant.getName());
        shopInfo.setPhone(tenant.getContactPersonPhone());
        shopInfo.setEmail(tenant.getContactPersonEmail());
        if (shopAddress != null && !shopAddress.isBlank()) {
            shopInfo.setAddress(shopAddress);
        }
        shopInfoRepository.save(shopInfo);
        log.debug("Seeded shop_info for tenant: {}", tenant.getTenantId());
    }

    private void seedDefaultConfig() {
        shopConfigService.seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.0);
        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, "STANDARD");
        shopConfigService.seedIfAbsent(ShopConfigKey.CASH_DENOMINATIONS, "1000,2000,5000,10000,20000,50000,100000,200000,500000");
        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_INTEREST_RATE, 0.0);
        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_INTEREST_TYPE, 30);
        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_DUE_DATE, 30);
        shopConfigService.seedIfAbsent(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false);
        log.debug("Seeded default shop_config");
    }

    private void seedShopOwnerUser(Tenant tenant, String adminUsername, String adminPassword) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("Admin user '{}' already exists in tenant: {}", adminUsername, tenant.getTenantId());
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
        // Use the owner-side set directly — avoids triggering the LAZY role.getUsers() collection,
        // which would fail if the role entity is detached from an earlier transaction.
        admin.getRoles().add(shopOwnerRole);
        userRepository.save(admin);
        log.info("Created admin user '{}' for tenant: {}", adminUsername, tenant.getTenantId());
    }
}

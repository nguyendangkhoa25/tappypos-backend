package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.tenant.InitialShopConfigRequest;
import com.tappy.pos.model.dto.tenant.RoleSetupRequest;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.enums.EmployeePosition;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.model.enums.ShopType;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.service.auth.RoleFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ShopConfigService shopConfigService;
    private final PasswordEncoder passwordEncoder;

    private static final Map<ShopType, String> SHOP_TYPE_WIDGET_DEFAULTS;
    static {
        Map<ShopType, String> m = new EnumMap<>(ShopType.class);
        m.put(ShopType.JEWELRY,            "ORDERS,REVENUE,PAWN,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.PAWN_SHOP,          "PAWN,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.CONVENIENCE_STORE,  "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.PHARMACY,           "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS");
        m.put(ShopType.ELECTRONICS,        "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.FOOD_BEVERAGE,      "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS");
        m.put(ShopType.FASHION,            "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.BARBER_SHOP,        "ORDERS,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.NAIL_SHOP,          "ORDERS,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.SPA_SHOP,           "ORDERS,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.COFFEE_SHOP,        "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.RESTAURANT,         "ORDERS,REVENUE,INVENTORY,EXPENSES,CUSTOMERS,EMPLOYEES");
        m.put(ShopType.OTHER,              "ORDERS,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        SHOP_TYPE_WIDGET_DEFAULTS = Collections.unmodifiableMap(m);
    }

    private static final Map<ShopType, String> SHOP_TYPE_NAV_DEFAULTS;
    static {
        Map<ShopType, String> m = new EnumMap<>(ShopType.class);
        m.put(ShopType.JEWELRY,            "home,pawn,pos,customers,orders,dashboard,users");
        m.put(ShopType.PAWN_SHOP,          "home,pawn,pos,customers,orders,dashboard,users");
        m.put(ShopType.CONVENIENCE_STORE,  "home,pos,orders,customers,dashboard,users");
        m.put(ShopType.PHARMACY,           "home,pos,orders,customers,dashboard,users");
        m.put(ShopType.ELECTRONICS,        "home,pos,orders,customers,dashboard,users");
        m.put(ShopType.FOOD_BEVERAGE,      "home,orders,pos,customers,dashboard,users");
        m.put(ShopType.FASHION,            "home,pos,orders,customers,dashboard,users");
        m.put(ShopType.BARBER_SHOP,        "home,orders,customers,dashboard,users");
        m.put(ShopType.NAIL_SHOP,          "home,orders,customers,dashboard,users");
        m.put(ShopType.SPA_SHOP,           "home,orders,customers,dashboard,users");
        m.put(ShopType.COFFEE_SHOP,        "home,orders,pos,customers,dashboard,users");
        m.put(ShopType.RESTAURANT,         "home,orders,pos,customers,dashboard,users");
        m.put(ShopType.OTHER,              "home,pos,orders,customers,dashboard,users");
        SHOP_TYPE_NAV_DEFAULTS = Collections.unmodifiableMap(m);
    }

    // Role → list of feature keys the role receives by default.
    // Profile intersection (applyShopTypeFilters) trims this list at provisioning time
    // so each shop type only gets features that make sense for it.
    private static final Map<String, List<String>> ROLE_FEATURES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(RoleEnum.SHOP_OWNER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "ORDER_VIEW_ALL", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "CUSTOMER", "LOYALTY",
            "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "TABLE_SERVICE", "ACTIVITY_LOG", "PAWN", "PAWN_VIEW_ALL", "GOLD_PRICE", "GOLD_PRICE_CHART",
            "COMMISSION", "COMMISSION_VIEW_ALL", "GOOGLE_DRIVE", "NOTIFICATION", "FEEDBACK", "APPOINTMENT", "BOOKING"
        ));
        m.put(RoleEnum.MANAGER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "ORDER_VIEW_ALL", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "CUSTOMER", "LOYALTY",
            "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "TABLE_SERVICE", "ACTIVITY_LOG", "PAWN", "PAWN_VIEW_ALL", "GOLD_PRICE", "GOLD_PRICE_CHART",
            "COMMISSION", "COMMISSION_VIEW_ALL", "NOTIFICATION", "FEEDBACK", "BOOKING"
        ));
        m.put(RoleEnum.CASHIER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "POS", "TABLE_SERVICE",
            "CUSTOMER", "LOYALTY", "PROMOTION", "COMMISSION",
            "NOTIFICATION", "FEEDBACK", "BOOKING"
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
            // PAWN_VIEW_ALL intentionally absent: PAWN_OFFICER sees only their own contracts.
            "DASHBOARD", "MY_WORK", "PAWN", "GOLD_PRICE", "GOLD_PRICE_CHART",
            "CUSTOMER", "LOYALTY", "ORDER", "POS", "PRODUCT",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.SERVICE_STAFF.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "POS", "TABLE_SERVICE",
            "CUSTOMER", "COMMISSION", "NOTIFICATION", "FEEDBACK", "BOOKING"
        ));
        m.put(RoleEnum.TECHNICIAN.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "PRODUCT", "CUSTOMER", "INVENTORY", "POS",
            "APPOINTMENT", "COMMISSION", "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.RECEPTIONIST.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "CUSTOMER", "POS", "TABLE_SERVICE",
            "APPOINTMENT", "COMMISSION", "NOTIFICATION", "FEEDBACK", "BOOKING"
        ));
        m.put(RoleEnum.CLEANER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "NOTIFICATION"
        ));
        // UTILITIES (client-side calculators/tools hub) is available to every role.
        // Which shop types actually expose it is decided by FEATURE_PROFILES below.
        m.replaceAll((role, feats) -> {
            List<String> withUtil = new ArrayList<>(feats);
            if (!withUtil.contains("UTILITIES")) withUtil.add("UTILITIES");
            return withUtil;
        });
        ROLE_FEATURES = m;
    }

    // ── Shop-type role whitelist ──────────────────────────────────────────────
    // Limits which roles are seeded at all for a given shop type.
    // Shop types NOT in this map get all roles (current behaviour for retail/F&B/etc.).
    private static final Map<ShopType, List<String>> SHOP_TYPE_ROLE_WHITELIST;
    static {
        Map<ShopType, List<String>> m = new EnumMap<>(ShopType.class);
        List<String> pawnRoles = Arrays.asList(
                RoleEnum.SHOP_OWNER.getCode(), RoleEnum.PAWN_OFFICER.getCode());
        List<String> jewelryRoles = Arrays.asList(
                RoleEnum.SHOP_OWNER.getCode(), RoleEnum.MANAGER.getCode(),
                RoleEnum.CASHIER.getCode(), RoleEnum.PAWN_OFFICER.getCode());
        m.put(ShopType.PAWN_SHOP, pawnRoles);
        m.put(ShopType.JEWELRY,   jewelryRoles);
        SHOP_TYPE_ROLE_WHITELIST = Collections.unmodifiableMap(m);
    }

    // ── Feature profiles ─────────────────────────────────────────────────────
    // Each profile defines the exact feature set meaningful for a category of shop.
    // Profile intersection ensures roles never receive features irrelevant to their shop.
    private static final Map<String, List<String>> FEATURE_PROFILES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("PAWN", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "PAWN", "PAWN_VIEW_ALL", "GOLD_PRICE", "GOLD_PRICE_CHART",
            "CUSTOMER", "LOYALTY", "APPOINTMENT",
            "ORDER", "ORDER_VIEW_ALL", "POS", "PRODUCT",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK"
        ));

        m.put("JEWELRY", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "PAWN", "PAWN_VIEW_ALL", "GOLD_PRICE", "GOLD_PRICE_CHART",
            "ORDER", "ORDER_VIEW_ALL", "POS", "PRODUCT", "INVENTORY", "VENDOR", "PROMOTION",
            "CUSTOMER", "LOYALTY", "APPOINTMENT",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "COMMISSION", "COMMISSION_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK"
        ));

        m.put("RETAIL", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "ORDER", "ORDER_VIEW_ALL", "POS",
            "PRODUCT", "INVENTORY", "VENDOR", "PROMOTION",
            "CUSTOMER", "LOYALTY", "APPOINTMENT",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "COMMISSION", "COMMISSION_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK"
        ));

        m.put("SERVICE", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "ORDER", "ORDER_VIEW_ALL", "POS",
            "PRODUCT", "PROMOTION",
            "CUSTOMER", "LOYALTY", "APPOINTMENT", "BOOKING",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "COMMISSION", "COMMISSION_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK"
        ));

        // Same as SERVICE + GOOGLE_DRIVE (before/after photos are core to spa/clinic workflows)
        m.put("BEAUTY", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "ORDER", "ORDER_VIEW_ALL", "POS",
            "PRODUCT", "PROMOTION",
            "CUSTOMER", "LOYALTY", "APPOINTMENT",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "COMMISSION", "COMMISSION_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK", "GOOGLE_DRIVE"
        ));

        m.put("FNB", Arrays.asList(
            "DASHBOARD", "MY_WORK",
            "ORDER", "ORDER_VIEW_ALL", "POS", "TABLE_SERVICE", "BOOKING",
            "PRODUCT", "INVENTORY", "VENDOR", "PROMOTION",
            "CUSTOMER", "LOYALTY", "APPOINTMENT",
            "REVENUE", "EXPENSE", "ACCOUNTING", "INVOICE",
            "EMPLOYEE", "SALARY", "SALARY_VIEW_ALL", "COMMISSION", "COMMISSION_VIEW_ALL",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "ACTIVITY_LOG",
            "NOTIFICATION", "FEEDBACK"
        ));

        // UTILITIES tools hub — scoped to jewelry / pawn / F&B shop types for now.
        for (String profile : Arrays.asList("PAWN", "JEWELRY", "FNB")) {
            List<String> feats = new ArrayList<>(m.get(profile));
            if (!feats.contains("UTILITIES")) feats.add("UTILITIES");
            m.put(profile, feats);
        }

        FEATURE_PROFILES = Collections.unmodifiableMap(m);
    }

    // Shop type → profile name. Types not in this map (e.g. OTHER) get all features.
    private static final Map<ShopType, String> SHOP_TYPE_FEATURE_PROFILE;
    static {
        Map<ShopType, String> m = new EnumMap<>(ShopType.class);
        m.put(ShopType.PAWN_SHOP,          "PAWN");
        m.put(ShopType.JEWELRY,            "JEWELRY");
        m.put(ShopType.CONVENIENCE_STORE,  "RETAIL");
        m.put(ShopType.PHARMACY,           "RETAIL");
        m.put(ShopType.ELECTRONICS,        "RETAIL");
        m.put(ShopType.FASHION,            "RETAIL");
        m.put(ShopType.BOOK_STORE,         "RETAIL");
        m.put(ShopType.BARBER_SHOP,        "SERVICE");
        m.put(ShopType.BARBER_SHOP_MEN,    "SERVICE");
        m.put(ShopType.HAIR_SALON,         "SERVICE");
        m.put(ShopType.NAIL_SHOP,          "SERVICE");
        m.put(ShopType.LASH_PMU_STUDIO,    "SERVICE");
        m.put(ShopType.MAKEUP_STUDIO,      "SERVICE");
        m.put(ShopType.MASSAGE_SHOP,       "SERVICE");
        m.put(ShopType.SPA_SHOP,           "BEAUTY");
        m.put(ShopType.BEAUTY_CLINIC,      "BEAUTY");
        m.put(ShopType.FOOD_BEVERAGE,      "FNB");
        m.put(ShopType.COFFEE_SHOP,        "FNB");
        m.put(ShopType.RESTAURANT,         "FNB");
        m.put(ShopType.PUB,                "FNB");
        m.put(ShopType.PUB_SEAFOOD,        "FNB");
        m.put(ShopType.PUB_GOAT,           "FNB");
        m.put(ShopType.PUB_BEEF,           "FNB");
        m.put(ShopType.BILLIARDS_HALL,     "FNB");      // POS + drinks/food orders; booking model TBD
        m.put(ShopType.TENNIS_COURT,       "SERVICE");  // service-style; booking model TBD
        // ShopType.OTHER → no entry → all features (safe default)
        SHOP_TYPE_FEATURE_PROFILE = Collections.unmodifiableMap(m);
    }

    /**
     * Provision a self-registered tenant by promoting the pre-provision user (tenant_id IS NULL)
     * to the new tenant scope. No second user row is created — the registration record is updated
     * in-place, keeping the same credential hash and ID.
     */
    @Transactional
    public void provisionSelfRegistered(Tenant tenant, String adminUsername, String shopAddress) {
        log.info("Provisioning self-registered tenant: {}", tenant.getTenantId());
        String tenantId = tenant.getTenantId();

        Map<String, List<String>> shopRoleFeatures =
                applyShopTypeFilters(ROLE_FEATURES, tenant.getShopType());
        // Seed roles through RoleFeatureService (its own @Transactional boundary) so the
        // RLS tenant context is set before the inserts — a direct seedRoles() here only
        // reliably creates SHOP_OWNER and silently drops the staff roles under FORCED RLS.
        roleFeatureService.seedRolesForTenant(new ArrayList<>(shopRoleFeatures.keySet()), tenantId);
        seedRoleFeatureMappings(shopRoleFeatures);

        try { seedShopInfo(tenant, shopAddress, tenantId); }
        catch (Exception e) { log.warn("seedShopInfo failed for {}: {}", tenantId, e.getMessage()); }

        try { seedDefaultConfig(tenant.getShopType()); }
        catch (Exception e) { log.warn("seedDefaultConfig failed for {}: {}", tenantId, e.getMessage()); }

        try { seedWalkInCustomer(tenantId); }
        catch (Exception e) { log.warn("seedWalkInCustomer failed for {}: {}", tenantId, e.getMessage()); }

        User adminUser = promoteUserToTenant(tenant, adminUsername, tenantId);

        try { seedShopOwnerEmployee(tenant, adminUser, tenantId); }
        catch (Exception e) { log.warn("seedShopOwnerEmployee failed for {}: {}", tenantId, e.getMessage()); }

        log.info("Self-registered provisioning complete for tenant: {}", tenantId);
    }

    /**
     * Promotes the pre-provision user (tenant_id IS NULL) to the target tenant.
     * Idempotent: if the user is already scoped to this tenant (retry scenario), returns as-is.
     */
    private User promoteUserToTenant(Tenant tenant, String adminUsername, String tenantId) {
        // Idempotency: already promoted in a previous attempt
        Optional<User> alreadyScoped = userRepository.findByUsernameTenantScoped(adminUsername);
        if (alreadyScoped.isPresent()) {
            log.info("User '{}' already promoted to tenant: {}", adminUsername, tenantId);
            return alreadyScoped.get();
        }

        User user = userRepository.findByUsernameAndNullTenant(adminUsername)
                .orElseThrow(() -> new RuntimeException("Pre-provision user not found: " + adminUsername));

        // Native lookup: the derived findByNameAndTenantId is corrupted by the tenantFilter
        // @Filter during provisioning and returns every role (NonUniqueResultException).
        Role shopOwnerRole = roleRepository.nativeFindByNameAndTenant(RoleEnum.SHOP_OWNER.getCode(), tenantId)
                .orElseThrow(() -> new RuntimeException("SHOP_OWNER role not found after seeding"));

        user.setTenantId(tenantId);
        if (tenant.getContactPersonName() != null && !tenant.getContactPersonName().isBlank()) {
            user.setFullName(tenant.getContactPersonName());
        }
        if (tenant.getContactPersonEmail() != null) {
            user.setEmail(tenant.getContactPersonEmail());
        }
        user.getRoles().add(shopOwnerRole);
        User saved = userRepository.save(user);
        log.info("Promoted user '{}' to tenant scope: {}", adminUsername, tenantId);
        return saved;
    }

    @Transactional
    public void provision(Tenant tenant, String adminUsername, String adminPassword,
                          List<RoleSetupRequest> roleSetups, String shopAddress,
                          InitialShopConfigRequest initialConfig) {
        log.info("Provisioning default data for tenant: {}", tenant.getTenantId());

        String tenantId = tenant.getTenantId();
        Map<String, List<String>> effectiveRoleFeatures = buildEffectiveRoleFeatures(roleSetups);

        seedRoles(effectiveRoleFeatures.keySet().stream().toList(), tenantId);
        seedRoleFeatureMappings(effectiveRoleFeatures);

        try { seedShopInfo(tenant, shopAddress, tenantId); }
        catch (Exception e) { log.warn("seedShopInfo failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { seedDefaultConfig(tenant.getShopType()); }
        catch (Exception e) { log.warn("seedDefaultConfig failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { applyInitialConfig(initialConfig); }
        catch (Exception e) { log.warn("applyInitialConfig failed for tenant {}: {}", tenantId, e.getMessage()); }

        try { seedWalkInCustomer(tenantId); }
        catch (Exception e) { log.warn("seedWalkInCustomer failed for tenant {}: {}", tenantId, e.getMessage()); }

        // Admin user must succeed — propagate failures to the caller.
        User adminUser = seedShopOwnerUser(tenant, adminUsername, adminPassword, tenantId);

        try { seedShopOwnerEmployee(tenant, adminUser, tenantId); }
        catch (Exception e) { log.warn("seedShopOwnerEmployee failed for tenant {}: {}", tenantId, e.getMessage()); }

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

    /**
     * Applies two shop-type-specific filters to the base role→feature map:
     * 1. Role whitelist  — keeps only roles valid for this shop type (e.g. PAWN_SHOP → SHOP_OWNER + PAWN_OFFICER only).
     * 2. Feature whitelist — strips features irrelevant to the shop type from every remaining role.
     * Shop types with no whitelist entry (e.g. OTHER) pass through unmodified.
     */
    private Map<String, List<String>> applyShopTypeFilters(Map<String, List<String>> base, ShopType shopType) {
        List<String> roleWhitelist    = SHOP_TYPE_ROLE_WHITELIST.get(shopType);    // null = allow all roles
        Set<String>  featureWhitelist = getProfileFeatureSet(shopType);             // null = allow all features

        Map<String, List<String>> result = new LinkedHashMap<>();
        base.forEach((role, features) -> {
            if (roleWhitelist != null && !roleWhitelist.contains(role)) return;
            List<String> filtered = featureWhitelist != null
                    ? features.stream().filter(featureWhitelist::contains).collect(Collectors.toList())
                    : new ArrayList<>(features);
            result.put(role, filtered);
        });
        return result;
    }

    /** Returns the feature set for the shop type's profile, or {@code null} if no filter applies (OTHER). */
    private Set<String> getProfileFeatureSet(ShopType shopType) {
        String profileName = SHOP_TYPE_FEATURE_PROFILE.get(shopType);
        if (profileName == null) return null;
        List<String> features = FEATURE_PROFILES.get(profileName);
        return features != null ? new HashSet<>(features) : null;
    }

    private void seedRoles(List<String> roleNames, String tenantId) {
        for (String roleName : roleNames) {
            RoleEnum roleEnum;
            try { roleEnum = RoleEnum.valueOf(roleName); }
            catch (IllegalArgumentException e) { continue; }
            if (!roleRepository.existsByNameAndTenantId(roleEnum.getCode(), tenantId)) {
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
        ShopInfo shopInfo = shopInfoRepository.findFirstByTenantIdAndDeletedAtIsNullOrderByIdAsc(tenantId)
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

    private void seedDefaultConfig(ShopType shopType) {
        // Jewellery shops are VAT-exempt in Vietnam; all other shop types default to 10 %.
        double defaultTaxRate = (shopType == ShopType.JEWELRY) ? 0.0 : 0.10;
        shopConfigService.seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, defaultTaxRate);
        shopConfigService.seedIfAbsent(ShopConfigKey.TAX_AUTO_APPLY, true);
        shopConfigService.seedIfAbsent(ShopConfigKey.POS_MODE, "STANDARD");
        String widgetDefault = SHOP_TYPE_WIDGET_DEFAULTS.getOrDefault(shopType,
                "ORDERS,REVENUE,EXPENSES,CUSTOMERS,EMPLOYEES");
        shopConfigService.seedIfAbsent(ShopConfigKey.DASHBOARD_WIDGETS, widgetDefault);
        String navDefault = SHOP_TYPE_NAV_DEFAULTS.getOrDefault(shopType,
                "home,pos,orders,customers,dashboard,users");
        shopConfigService.seedIfAbsent(ShopConfigKey.NAV_CONFIG, navDefault);
        log.debug("Seeded default shop_config for shopType: {}", shopType);
    }

    private void applyInitialConfig(InitialShopConfigRequest cfg) {
        if (cfg == null) return;
        if (cfg.getPosMode() != null && !cfg.getPosMode().isBlank()) {
            shopConfigService.set(ShopConfigKey.POS_MODE, cfg.getPosMode());
            log.debug("Applied initial POS_MODE: {}", cfg.getPosMode());
        }
        if (cfg.getPawnCategoryConfig() != null && !cfg.getPawnCategoryConfig().isBlank()) {
            shopConfigService.set(ShopConfigKey.PAWN_CATEGORY_CONFIG, cfg.getPawnCategoryConfig());
            log.debug("Applied initial PAWN_CATEGORY_CONFIG");
        }
    }

    private void seedWalkInCustomer(String tenantId) {
        if (customerRepository.findByPhoneAndTenantId("0000000000", tenantId).isPresent()) {
            log.debug("Walk-in customer already exists for tenant: {}", tenantId);
            return;
        }
        Customer walkIn = Customer.builder()
                .name("Khách lẻ")
                .phone("0000000000")
                .notes("Khách hàng lẻ - không có thông tin liên hệ")
                .walkIn(true)
                .build();
        walkIn.setTenantId(tenantId);
        customerRepository.save(walkIn);
        log.debug("Seeded walk-in customer for tenant: {}", tenantId);
    }

    private User seedShopOwnerUser(Tenant tenant, String adminUsername, String adminPassword, String tenantId) {
        Optional<User> existingUser = userRepository.findByUsernameTenantScoped(adminUsername);
        if (existingUser.isPresent()) {
            log.info("Admin user '{}' already exists in tenant: {}", adminUsername, tenantId);
            return existingUser.get();
        }

        // Native lookup: the derived findByNameAndTenantId is corrupted by the tenantFilter
        // @Filter during provisioning and returns every role (NonUniqueResultException).
        Role shopOwnerRole = roleRepository.nativeFindByNameAndTenant(RoleEnum.SHOP_OWNER.getCode(), tenantId)
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
        User saved = userRepository.save(admin);
        log.info("Created admin user '{}' for tenant: {}", adminUsername, tenantId);
        return saved;
    }

    private void seedShopOwnerEmployee(Tenant tenant, User adminUser, String tenantId) {
        if (employeeRepository.existsByUserId(adminUser.getId())) {
            log.debug("Employee already linked to admin user {} in tenant: {}", adminUser.getId(), tenantId);
            return;
        }
        String name = tenant.getContactPersonName() != null && !tenant.getContactPersonName().isBlank()
                ? tenant.getContactPersonName() : "Chủ cửa hàng";
        Employee employee = Employee.builder()
                .fullName(name)
                .phone(tenant.getContactPersonPhone())
                .email(tenant.getContactPersonEmail())
                .position(EmployeePosition.SHOP_OWNER)
                .hireDate(LocalDate.now())
                .active(true)
                .userId(adminUser.getId())
                .build();
        employee.setTenantId(tenantId);
        employeeRepository.save(employee);
        log.info("Seeded SHOP_OWNER employee '{}' linked to user '{}' for tenant: {}",
                name, adminUser.getUsername(), tenantId);
    }

    /**
     * Creates an Employee record for a user who just joined a shop via invitation code.
     * Staff need an employee record to be assigned work items / earn commission
     * (My Work, etc. resolve the current employee from the user). Runs on its own
     * @Transactional boundary so TenantRlsAspect sets app.current_tenant for the insert.
     */
    @Transactional
    public void seedJoinedStaffEmployee(User user, String tenantId, String roleName) {
        if (employeeRepository.existsByUserId(user.getId())) {
            log.debug("Employee already linked to user {} in tenant: {}", user.getId(), tenantId);
            return;
        }
        EmployeePosition position = null;
        try { position = EmployeePosition.valueOf(roleName); } catch (IllegalArgumentException ignored) { }
        String name = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : (user.getNickname() != null && !user.getNickname().isBlank()
                        ? user.getNickname() : user.getUsername());
        Employee employee = Employee.builder()
                .fullName(name)
                .phone(user.getPhone())
                .email(user.getEmail())
                .position(position)
                .hireDate(LocalDate.now())
                .active(true)
                .userId(user.getId())
                .build();
        employee.setTenantId(tenantId);
        employeeRepository.save(employee);
        log.info("Seeded staff employee '{}' (role {}) linked to user '{}' for tenant: {}",
                name, roleName, user.getUsername(), tenantId);
    }
}

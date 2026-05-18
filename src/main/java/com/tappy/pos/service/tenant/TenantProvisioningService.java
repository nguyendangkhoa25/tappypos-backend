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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        m.put(ShopType.PAWN_SHOP,          "home,pawn,customers,orders,dashboard,users");
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

    // Role → list of feature keys the role receives by default
    private static final Map<String, List<String>> ROLE_FEATURES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(RoleEnum.SHOP_OWNER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "ORDER_VIEW_ALL", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "SALARY", "CUSTOMER", "LOYALTY", "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "ACTIVITY_LOG", "PAWN", "COMMISSION", "NOTIFICATION", "FEEDBACK", "APPOINTMENT"
        ));
        m.put(RoleEnum.MANAGER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "CUSTOMER", "LOYALTY", "INVOICE", "ACCOUNTING", "REVENUE", "EXPENSE",
            "USER", "SHOP_INFO", "PRINT_TEMPLATE", "BANK_ACCOUNT", "VENDOR", "INVENTORY", "POS",
            "ACTIVITY_LOG", "PAWN", "COMMISSION", "NOTIFICATION", "FEEDBACK"
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
            "DASHBOARD", "MY_WORK", "ORDER", "POS", "CUSTOMER", "COMMISSION",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.TECHNICIAN.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "PRODUCT", "CUSTOMER", "INVENTORY", "POS", "PAWN",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.RECEPTIONIST.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "ORDER", "CUSTOMER", "POS", "COMMISSION",
            "NOTIFICATION", "FEEDBACK"
        ));
        m.put(RoleEnum.CLEANER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK", "NOTIFICATION"
        ));
        ROLE_FEATURES = m;
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

        seedRoles(new java.util.ArrayList<>(ROLE_FEATURES.keySet()), tenantId);
        seedRoleFeatureMappings(ROLE_FEATURES);

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

        Role shopOwnerRole = roleRepository.findByNameAndTenantId(RoleEnum.SHOP_OWNER.getCode(), tenantId)
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

        Role shopOwnerRole = roleRepository.findByNameAndTenantId(RoleEnum.SHOP_OWNER.getCode(), tenantId)
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
}

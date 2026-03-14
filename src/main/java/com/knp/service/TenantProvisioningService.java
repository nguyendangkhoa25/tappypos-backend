package com.knp.service;

import com.knp.model.entity.Customer;
import com.knp.model.entity.Feature;
import com.knp.model.entity.Role;
import com.knp.model.entity.ShopInfo;
import com.knp.model.entity.Tenant;
import com.knp.model.entity.User;
import com.knp.model.enums.FeatureEnum;
import com.knp.model.enums.RoleEnum;
import com.knp.repository.CustomerRepository;
import com.knp.repository.RoleFeatureRepository;
import com.knp.repository.RoleRepository;
import com.knp.repository.ShopInfoRepository;
import com.knp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Seeds default roles, features, admin user, shop info and walk-in customer
 * into a newly provisioned tenant database.
 *
 * Must be called with TenantContext already set to the target tenant so that
 * all JPA operations are routed to the correct tenant datasource.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final RoleRepository roleRepository;
    private final RoleFeatureRepository roleFeatureRepository;
    private final UserRepository userRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    // Role → list of feature keys the role receives by default
    private static final Map<String, List<String>> ROLE_FEATURES = Map.of(
        RoleEnum.SHOP_OWNER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "SALARY", "CUSTOMER", "INVOICE", "REVENUE",
            "USER", "SHOP_INFO", "VENDOR", "INVENTORY", "POS", "ACTIVITY_LOG"
        ),
        RoleEnum.MANAGER.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "PROMOTION",
            "EMPLOYEE", "CUSTOMER", "INVOICE", "REVENUE",
            "USER", "VENDOR", "INVENTORY", "POS", "ACTIVITY_LOG"
        ),
        RoleEnum.TECHNICIAN.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "PRODUCT", "CUSTOMER", "INVENTORY", "POS"
        ),
        RoleEnum.RECEPTIONIST.getCode(), Arrays.asList(
            "DASHBOARD", "ORDER", "MY_WORK", "CUSTOMER", "POS"
        ),
        RoleEnum.CLEANER.getCode(), Arrays.asList(
            "DASHBOARD", "MY_WORK"
        )
    );

    @Transactional
    public void provision(Tenant tenant, String adminUsername, String adminPassword) {
        log.info("Provisioning default data for tenant: {}", tenant.getTenantId());

        seedRoles();
        seedFeatures();
        seedRoleFeatureMappings();
        createShopInfo(tenant);
        createAdminUser(tenant, adminUsername, adminPassword);
        createWalkInCustomer();

        log.info("Provisioning complete for tenant: {}", tenant.getTenantId());
    }

    private void seedRoles() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum == RoleEnum.MASTER_TENANT) continue;
            if (!roleRepository.existsByName(roleEnum.getCode())) {
                roleRepository.save(new Role(roleEnum.getCode(), roleEnum.getDescription()));
                log.debug("Seeded role: {}", roleEnum.getCode());
            }
        }
    }

    private void seedFeatures() {
        for (FeatureEnum featureEnum : FeatureEnum.values()) {
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

    private void seedRoleFeatureMappings() {
        ROLE_FEATURES.forEach((roleName, features) ->
            features.forEach(featureName -> {
                try {
                    roleFeatureRepository.assignFeatureToRole(roleName, featureName);
                } catch (Exception e) {
                    log.warn("Could not assign feature {} to role {}: {}", featureName, roleName, e.getMessage());
                }
            })
        );
    }

    private void createShopInfo(Tenant tenant) {
        if (shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc().isEmpty()) {
            ShopInfo shopInfo = ShopInfo.builder()
                    .shopName(tenant.getName())
                    .phone(tenant.getContactPersonPhone())
                    .email(tenant.getContactPersonEmail())
                    .defaultTaxRate(0.0)
                    .posMode("STANDARD")
                    .cashDenominations("1000,2000,5000,10000,20000,50000,100000,200000,500000")
                    .build();
            shopInfoRepository.save(shopInfo);
            log.debug("Seeded shop_info for tenant: {}", tenant.getTenantId());
        }
    }

    private void createAdminUser(Tenant tenant, String adminUsername, String adminPassword) {
        String username = (adminUsername != null && !adminUsername.isBlank()) ? adminUsername : "admin";
        String password = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : "Admin@123";

        if (userRepository.findByUsername(username).isPresent()) {
            log.debug("Admin user '{}' already exists in tenant: {}", username, tenant.getTenantId());
            return;
        }

        Role shopOwnerRole = roleRepository.findByName(RoleEnum.SHOP_OWNER.getCode())
                .orElseThrow(() -> new RuntimeException("SHOP_OWNER role not found after seeding"));

        User admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(tenant.getContactPersonName() != null ? tenant.getContactPersonName() : "Admin")
                .email(tenant.getContactPersonEmail())
                .active(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .requireAction(null)
                .lang("vi")
                .build();
        admin.addRole(shopOwnerRole);
        userRepository.save(admin);
        log.debug("Created admin user '{}' for tenant: {}", username, tenant.getTenantId());
    }

    private void createWalkInCustomer() {
        String walkInPhone = "0000000000";
        if (customerRepository.findByPhone(walkInPhone).isEmpty()) {
            Customer walkIn = Customer.builder()
                    .name("Khách lẻ")
                    .phone(walkInPhone)
                    .loyaltyPoints(0)
                    .totalSpent(java.math.BigDecimal.ZERO)
                    .build();
            customerRepository.save(walkIn);
            log.debug("Seeded walk-in customer");
        }
    }
}

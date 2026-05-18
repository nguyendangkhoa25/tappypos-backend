package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.tenant.InitialShopConfigRequest;
import com.tappy.pos.model.dto.tenant.RoleSetupRequest;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.model.enums.ShopType;
import com.tappy.pos.repository.auth.RoleRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.auth.RoleFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.nullable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantProvisioningService Unit Tests")
class TenantProvisioningServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RoleFeatureService roleFeatureService;
    @Mock private UserRepository userRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantProvisioningService tenantProvisioningService;

    private Tenant tenant;
    private Role shopOwnerRole;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantId("test-shop");
        tenant.setName("Tiệm Vàng ABC");
        tenant.setContactPersonName("Nguyễn Văn A");
        tenant.setContactPersonPhone("0901234567");
        tenant.setContactPersonEmail("abc@example.com");

        shopOwnerRole = new Role(RoleEnum.SHOP_OWNER.getCode(), "Shop Owner");
        shopOwnerRole.setId(1L);

        lenient().when(roleRepository.existsByNameAndTenantId(anyString(), anyString())).thenReturn(false);
        lenient().when(roleRepository.save(any(Role.class))).thenReturn(shopOwnerRole);
        lenient().when(roleRepository.findByNameAndTenantId(RoleEnum.SHOP_OWNER.getCode(), "test-shop"))
                .thenReturn(Optional.of(shopOwnerRole));
        lenient().when(shopInfoRepository.findFirstByTenantIdAndDeletedAtIsNullOrderByIdAsc("test-shop"))
                .thenReturn(Optional.empty());
        lenient().when(shopInfoRepository.save(any(ShopInfo.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(customerRepository.findByPhoneAndTenantId("0000000000", "test-shop"))
                .thenReturn(Optional.empty());
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(userRepository.findByUsernameTenantScoped("admin")).thenReturn(Optional.empty());
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        lenient().when(employeeRepository.existsByUserId(nullable(Long.class))).thenReturn(false);
        lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── provision ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("provision seeds roles, shop info, customer, and admin user")
    void testProvision_FullRun() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, "123 Đường ABC", null);

        verify(roleRepository, atLeastOnce()).save(any(Role.class));
        verify(shopInfoRepository).save(any(ShopInfo.class));
        verify(customerRepository).save(any(Customer.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("provision seeds shop info with provided address")
    void testProvision_SeedsShopInfoWithAddress() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, "123 Đường Test", null);

        verify(shopInfoRepository).save(argThat(info -> "123 Đường Test".equals(info.getAddress())));
    }

    @Test
    @DisplayName("provision seeds shop info without address when null")
    void testProvision_SeedsShopInfoWithoutAddress() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("provision seeds walk-in customer with phone 0000000000")
    void testProvision_SeedsWalkInCustomer() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(customerRepository).save(argThat(c -> "0000000000".equals(c.getPhone())));
    }

    @Test
    @DisplayName("provision skips walk-in customer if already exists")
    void testProvision_SkipsExistingWalkInCustomer() {
        when(customerRepository.findByPhoneAndTenantId("0000000000", "test-shop"))
                .thenReturn(Optional.of(new Customer()));

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("provision seeds admin user with encoded password")
    void testProvision_SeedsAdminUser() {
        when(passwordEncoder.encode("mypassword")).thenReturn("hashed");

        tenantProvisioningService.provision(tenant, "admin", "mypassword", null, null, null);

        verify(userRepository).save(argThat(u ->
                "admin".equals(u.getUsername()) && "hashed".equals(u.getPassword())));
    }

    @Test
    @DisplayName("provision skips admin user if already exists")
    void testProvision_SkipsExistingAdminUser() {
        User existing = User.builder().username("admin").build();
        when(userRepository.findByUsernameTenantScoped("admin")).thenReturn(Optional.of(existing));

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("provision throws when SHOP_OWNER role not found after seeding")
    void testProvision_ThrowsWhenShopOwnerRoleMissing() {
        when(roleRepository.findByNameAndTenantId(RoleEnum.SHOP_OWNER.getCode(), "test-shop"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tenantProvisioningService.provision(tenant, "admin", "password", null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SHOP_OWNER");
    }

    @Test
    @DisplayName("provision seeds default shop config keys")
    void testProvision_SeedsDefaultConfig() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(shopConfigService).seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.1);
        verify(shopConfigService).seedIfAbsent(ShopConfigKey.POS_MODE, "STANDARD");
    }

    @Test
    @DisplayName("provision updates existing shop info if already present")
    void testProvision_UpdatesExistingShopInfo() {
        ShopInfo existing = ShopInfo.builder().build();
        existing.setTenantId("test-shop");
        when(shopInfoRepository.findFirstByTenantIdAndDeletedAtIsNullOrderByIdAsc("test-shop"))
                .thenReturn(Optional.of(existing));

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(shopInfoRepository).save(argThat(info -> "Tiệm Vàng ABC".equals(info.getShopName())));
    }

    @Test
    @DisplayName("provision does not seed existing roles")
    void testProvision_SkipsAlreadyExistingRole() {
        when(roleRepository.existsByNameAndTenantId(RoleEnum.SHOP_OWNER.getCode(), "test-shop"))
                .thenReturn(true);

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(roleRepository, never()).save(argThat(r ->
                RoleEnum.SHOP_OWNER.getCode().equals(r.getName())));
    }

    // ── buildEffectiveRoleFeatures ─────────────────────────────────────────────

    @Test
    @DisplayName("provision with custom roleSetups applies custom feature list")
    void testProvision_WithCustomRoleSetups_AppliesCustomFeatures() {
        RoleSetupRequest setup = new RoleSetupRequest("CASHIER", List.of("POS", "ORDER"));
        tenantProvisioningService.provision(tenant, "admin", "password",
                List.of(setup), null, null);

        verify(roleFeatureService).setRoleFeatures("CASHIER", List.of("POS", "ORDER"));
    }

    @Test
    @DisplayName("provision with custom roleSetups uses default features when setup.features is null")
    void testProvision_WithCustomRoleSetups_NullFeatures_UsesDefault() {
        RoleSetupRequest setup = new RoleSetupRequest("CASHIER", null);
        tenantProvisioningService.provision(tenant, "admin", "password",
                List.of(setup), null, null);

        verify(roleFeatureService).setRoleFeatures(eq("CASHIER"), argThat(f -> !f.isEmpty()));
    }

    @Test
    @DisplayName("provision with roleSetups skips MASTER_TENANT role")
    void testProvision_WithCustomRoleSetups_SkipsMasterRole() {
        RoleSetupRequest masterSetup = new RoleSetupRequest("MASTER_TENANT", List.of("USER"));
        RoleSetupRequest cashierSetup = new RoleSetupRequest("CASHIER", List.of("POS"));
        tenantProvisioningService.provision(tenant, "admin", "password",
                List.of(masterSetup, cashierSetup), null, null);

        verify(roleFeatureService, never()).setRoleFeatures(eq("MASTER_TENANT"), any());
        verify(roleFeatureService).setRoleFeatures("CASHIER", List.of("POS"));
    }

    @Test
    @DisplayName("provision with roleSetups skips entries with blank roleName")
    void testProvision_WithCustomRoleSetups_SkipsBlankRoleName() {
        RoleSetupRequest blankSetup = new RoleSetupRequest("", List.of("POS"));
        RoleSetupRequest validSetup = new RoleSetupRequest("CASHIER", List.of("POS"));
        tenantProvisioningService.provision(tenant, "admin", "password",
                List.of(blankSetup, validSetup), null, null);

        verify(roleFeatureService).setRoleFeatures("CASHIER", List.of("POS"));
    }

    // ── applyInitialConfig ─────────────────────────────────────────────────────

    @Test
    @DisplayName("provision with initialConfig sets POS_MODE")
    void testProvision_WithInitialConfig_SetsPosMode() {
        InitialShopConfigRequest cfg = InitialShopConfigRequest.builder()
                .posMode("TABLE")
                .build();

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, cfg);

        verify(shopConfigService).set(ShopConfigKey.POS_MODE, "TABLE");
    }

    @Test
    @DisplayName("provision with initialConfig sets PAWN_CATEGORY_CONFIG")
    void testProvision_WithInitialConfig_SetsPawnConfig() {
        InitialShopConfigRequest cfg = InitialShopConfigRequest.builder()
                .pawnCategoryConfig("{\"enabled\":[\"GOLD\"]}")
                .build();

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, cfg);

        verify(shopConfigService).set(ShopConfigKey.PAWN_CATEGORY_CONFIG, "{\"enabled\":[\"GOLD\"]}");
    }

    @Test
    @DisplayName("provision with blank posMode in config does not call shopConfigService.set")
    void testProvision_WithInitialConfig_BlankPosMode_DoesNotSet() {
        InitialShopConfigRequest cfg = InitialShopConfigRequest.builder()
                .posMode("  ")
                .build();

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, cfg);

        verify(shopConfigService, never()).set(eq(ShopConfigKey.POS_MODE), anyString());
    }

    // ── seedShopOwnerEmployee ──────────────────────────────────────────────────

    @Test
    @DisplayName("provision seeds SHOP_OWNER employee linked to admin user")
    void testProvision_SeedsShopOwnerEmployee_WhenNotExists() {
        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(employeeRepository).save(argThat(e ->
                "Nguyễn Văn A".equals(e.getFullName()) &&
                e.getPosition() != null));
    }

    @Test
    @DisplayName("provision skips employee creation when already linked to admin user")
    void testProvision_SeedsShopOwnerEmployee_SkipsWhenExists() {
        when(employeeRepository.existsByUserId(nullable(Long.class))).thenReturn(true);

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ── seedDefaultConfig with JEWELRY shop type ───────────────────────────────

    @Test
    @DisplayName("provision seeds zero tax rate for JEWELRY shop type")
    void testProvision_JewelryShopType_ZeroTaxRate() {
        tenant.setShopType(ShopType.JEWELRY);

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(shopConfigService).seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.0);
    }

    @Test
    @DisplayName("provision seeds 10% tax rate for non-JEWELRY shop types")
    void testProvision_NonJewelryShopType_TenPercentTaxRate() {
        tenant.setShopType(ShopType.CONVENIENCE_STORE);

        tenantProvisioningService.provision(tenant, "admin", "password", null, null, null);

        verify(shopConfigService).seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.10);
    }
}

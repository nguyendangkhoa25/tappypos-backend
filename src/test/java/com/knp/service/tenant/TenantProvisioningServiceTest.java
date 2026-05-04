package com.knp.service.tenant;

import com.knp.model.entity.auth.Role;
import com.knp.model.entity.auth.User;
import com.knp.model.entity.customer.Customer;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.enums.RoleEnum;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.auth.RoleRepository;
import com.knp.repository.auth.UserRepository;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.service.auth.RoleFeatureService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantProvisioningService Unit Tests")
class TenantProvisioningServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RoleFeatureService roleFeatureService;
    @Mock private UserRepository userRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private CustomerRepository customerRepository;
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

        verify(shopConfigService).seedIfAbsent(ShopConfigKey.DEFAULT_TAX_RATE, 0.0);
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
}

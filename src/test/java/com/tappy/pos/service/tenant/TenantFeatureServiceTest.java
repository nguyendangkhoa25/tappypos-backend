package com.tappy.pos.service.tenant;

import com.tappy.pos.model.entity.auth.Feature;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleFeatureRepository;
import com.tappy.pos.repository.auth.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantFeatureService Unit Tests")
class TenantFeatureServiceTest {

    @Mock
    private RoleFeatureRepository roleFeatureRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private TenantFeatureService tenantFeatureService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .tenantId("tenant-123")
                .name("Test Tenant")
                .dbName("tenant_db")
                .active(true)
                .features("DASHBOARD,ORDER,CUSTOMER")
                .build();
    }

    // ============= getAccessibleFeaturesByRoleAndTenant Tests =============

    @Test
    @DisplayName("Should get accessible features based on tenant and role intersection")
    void testGetAccessibleFeaturesByRoleAndTenant_Success() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER", "MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // Intersection of DASHBOARD, ORDER, CUSTOMER
        assertThat(result).contains("DASHBOARD", "ORDER", "CUSTOMER");
        assertThat(result).doesNotContain("REPORT");
        verify(tenantContext).getCurrentTenant();
        verify(roleFeatureRepository).findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString());
    }

    @Test
    @DisplayName("Should return all role features when tenant is null")
    void testGetAccessibleFeaturesByRoleAndTenant_NoTenant() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(null);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isEqualTo(roleFeatures);
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("Should return empty features when role features are empty")
    void testGetAccessibleFeaturesByRoleAndTenant_NoRoleFeatures() {
        // Given
        List<String> roleNames = Collections.emptyList();

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when tenant has no features")
    void testGetAccessibleFeaturesByRoleAndTenant_TenantNoFeatures() {
        // Given
        Tenant emptyTenant = Tenant.builder()
                .id(2L)
                .tenantId("empty-tenant")
                .features("")
                .build();
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(emptyTenant);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle partial intersection of features")
    void testGetAccessibleFeaturesByRoleAndTenant_PartialIntersection() {
        // Given
        Tenant limitedTenant = Tenant.builder()
                .tenantId("limited")
                .features("DASHBOARD,CUSTOMER")
                .build();
        List<String> roleNames = Arrays.asList("MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(limitedTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains("DASHBOARD", "CUSTOMER");
        assertThat(result).doesNotContain("ORDER", "REPORT");
    }

    @Test
    @DisplayName("Should handle whitespace in tenant features")
    void testGetAccessibleFeaturesByRoleAndTenant_WhitespaceFeatures() {
        // Given
        Tenant spacedTenant = Tenant.builder()
                .tenantId("spaced")
                .features("DASHBOARD , ORDER , CUSTOMER")
                .build();
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(spacedTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains("DASHBOARD", "ORDER", "CUSTOMER");
    }

    // ============= getTenantAssignedFeatures Tests =============

    @Test
    @DisplayName("Should get tenant assigned features successfully")
    void testGetTenantAssignedFeatures_Success() {
        // Given
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        Set<String> result = tenantFeatureService.getTenantAssignedFeatures();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).contains("DASHBOARD", "ORDER", "CUSTOMER");
        verify(tenantContext).getCurrentTenant();
    }

    @Test
    @DisplayName("Should return empty set when tenant is null")
    void testGetTenantAssignedFeatures_NoTenant() {
        // Given
        when(tenantContext.getCurrentTenant()).thenReturn(null);

        // When
        Set<String> result = tenantFeatureService.getTenantAssignedFeatures();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when tenant has no features")
    void testGetTenantAssignedFeatures_NoFeatures() {
        // Given
        Tenant emptyTenant = Tenant.builder()
                .tenantId("empty")
                .features(null)
                .build();
        when(tenantContext.getCurrentTenant()).thenReturn(emptyTenant);

        // When
        Set<String> result = tenantFeatureService.getTenantAssignedFeatures();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when tenant features is empty string")
    void testGetTenantAssignedFeatures_EmptyString() {
        // Given
        Tenant emptyTenant = Tenant.builder()
                .tenantId("empty")
                .features("")
                .build();
        when(tenantContext.getCurrentTenant()).thenReturn(emptyTenant);

        // When
        Set<String> result = tenantFeatureService.getTenantAssignedFeatures();

        // Then
        assertThat(result).isEmpty();
    }

    // ============= hasAccessToFeature Tests =============

    @Test
    @DisplayName("Should return true when user has access to feature")
    void testHasAccessToFeature_HasAccess() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        boolean result = tenantFeatureService.hasAccessToFeature(roleNames, "DASHBOARD");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user does not have access to feature")
    void testHasAccessToFeature_NoAccess() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        boolean result = tenantFeatureService.hasAccessToFeature(roleNames, "REPORT");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should check single role access to feature")
    void testHasAccessToFeature_SingleRole() {
        // Given
        String roleName = "MANAGER";
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(List.of(roleName)), anyString()))
                .thenReturn(roleFeatures);

        // When
        boolean result = tenantFeatureService.hasAccessToFeature(roleName, "DASHBOARD");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for feature not in tenant")
    void testHasAccessToFeature_FeatureNotInTenant() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        boolean result = tenantFeatureService.hasAccessToFeature(roleNames, "REPORT");

        // Then
        assertThat(result).isFalse();
    }

    // ============= validateTenantHasFeatures Tests =============

    @Test
    @DisplayName("Should validate tenant has all requested features")
    void testValidateTenantHasFeatures_Success() {
        // Given
        List<String> requestedFeatures = Arrays.asList("DASHBOARD", "ORDER");
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        boolean result = tenantFeatureService.validateTenantHasFeatures(requestedFeatures);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when tenant missing features")
    void testValidateTenantHasFeatures_MissingFeatures() {
        // Given
        List<String> requestedFeatures = Arrays.asList("DASHBOARD", "REPORT");
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        boolean result = tenantFeatureService.validateTenantHasFeatures(requestedFeatures);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should validate empty requested features")
    void testValidateTenantHasFeatures_EmptyRequest() {
        // Given
        List<String> requestedFeatures = Collections.emptyList();
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        boolean result = tenantFeatureService.validateTenantHasFeatures(requestedFeatures);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate all tenant features")
    void testValidateTenantHasFeatures_AllFeatures() {
        // Given
        List<String> requestedFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        boolean result = tenantFeatureService.validateTenantHasFeatures(requestedFeatures);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for null tenant")
    void testValidateTenantHasFeatures_NullTenant() {
        // Given
        List<String> requestedFeatures = Arrays.asList("DASHBOARD");
        when(tenantContext.getCurrentTenant()).thenReturn(null);

        // When
        boolean result = tenantFeatureService.validateTenantHasFeatures(requestedFeatures);

        // Then
        assertThat(result).isFalse();
    }

    // ============= Edge Case Tests =============

    @Test
    @DisplayName("Should handle case sensitivity in features")
    void testGetAccessibleFeaturesByRoleAndTenant_CaseSensitive() {
        // Given
        // Tenant has: "dashboard", "ORDER", "customer"  (mixed case)
        // Roles have: "DASHBOARD", "ORDER", "CUSTOMER"  (all uppercase)
        // Only "ORDER" matches (exact case match), others don't match
        Tenant caseTenant = Tenant.builder()
                .tenantId("case")
                .features("dashboard,ORDER,customer")  // lowercase and mixed case
                .build();
        List<String> roleNames = List.of("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");  // uppercase

        when(tenantContext.getCurrentTenant()).thenReturn(caseTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then - case sensitive: only "ORDER" matches (exact case), others don't
        assertThat(result).hasSize(1);
        assertThat(result).contains("ORDER");
        assertThat(result).doesNotContain("DASHBOARD", "CUSTOMER");
    }

    @Test
    @DisplayName("Should handle single feature in tenant")
    void testGetAccessibleFeaturesByRoleAndTenant_SingleFeature() {
        // Given
        Tenant singleTenant = Tenant.builder()
                .tenantId("single")
                .features("DASHBOARD")
                .build();
        List<String> roleNames = Arrays.asList("MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(singleTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains("DASHBOARD");
    }

    @Test
    @DisplayName("Should handle many features in tenant")
    void testGetAccessibleFeaturesByRoleAndTenant_ManyFeatures() {
        // Given
        Tenant manyTenant = Tenant.builder()
                .tenantId("many")
                .features("DASHBOARD,ORDER,CUSTOMER,REPORT,SETTINGS,PRODUCT,INVENTORY,PAYMENT")
                .build();
        List<String> roleNames = Arrays.asList("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT", "SETTINGS");

        when(tenantContext.getCurrentTenant()).thenReturn(manyTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("Should handle zero intersection")
    void testGetAccessibleFeaturesByRoleAndTenant_ZeroIntersection() {
        // Given
        Tenant otherTenant = Tenant.builder()
                .tenantId("other")
                .features("PAYMENT,SHIPPING")
                .build();
        List<String> roleNames = Arrays.asList("MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(otherTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle whitespace-only features string")
    void testGetAccessibleFeaturesByRoleAndTenant_WhitespaceOnly() {
        // Given
        Tenant whitespaceTenant = Tenant.builder()
                .tenantId("whitespace")
                .features("   ,   ,   ")
                .build();
        List<String> roleNames = Arrays.asList("MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD");

        when(tenantContext.getCurrentTenant()).thenReturn(whitespaceTenant);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByRoleAndTenant(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    // ============= getAccessibleFeaturesByUserAndTenant Tests =============

    @Test
    @DisplayName("Should use user-specific features intersected with tenant when override is set")
    void testGetAccessibleFeaturesByUserAndTenant_UserOverride() {
        // Given: user has explicit features DASHBOARD, ORDER, REPORT; tenant has DASHBOARD, ORDER, CUSTOMER
        User user = User.builder()
                .username("alice")
                .userFeatures(Set.of(
                        feature("DASHBOARD"), feature("ORDER"), feature("REPORT")))
                .build();
        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByUserAndTenant(
                user, List.of("SHOP_OWNER"));

        // Then: intersection of user override and tenant = DASHBOARD, ORDER (REPORT not in tenant)
        assertThat(result).containsExactlyInAnyOrder("DASHBOARD", "ORDER");
        assertThat(result).doesNotContain("REPORT", "CUSTOMER");
        // role-based path must NOT be consulted when override exists
        verify(roleFeatureRepository, never())
                .findActiveFeatureNamesByRoleNamesAndTenantId(eq(List.of("SHOP_OWNER")), anyString());
    }

    @Test
    @DisplayName("Should return raw user features when override is set but no tenant context")
    void testGetAccessibleFeaturesByUserAndTenant_UserOverrideNoTenant() {
        // Given
        User user = User.builder()
                .username("bob")
                .userFeatures(Set.of(feature("DASHBOARD"), feature("ORDER")))
                .build();
        when(tenantContext.getCurrentTenant()).thenReturn(null);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByUserAndTenant(
                user, List.of("SHOP_OWNER"));

        // Then: returns the full user override unfiltered
        assertThat(result).containsExactlyInAnyOrder("DASHBOARD", "ORDER");
    }

    @Test
    @DisplayName("Should fall back to role-based resolution when user has no override features")
    void testGetAccessibleFeaturesByUserAndTenant_FallbackToRole() {
        // Given: user with empty userFeatures
        User user = User.builder()
                .username("carol")
                .userFeatures(Collections.emptySet())
                .build();
        List<String> roleNames = List.of("MANAGER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "REPORT");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByUserAndTenant(user, roleNames);

        // Then: tenant (DASHBOARD,ORDER,CUSTOMER) ∩ role (DASHBOARD,ORDER,REPORT) = DASHBOARD, ORDER
        assertThat(result).containsExactlyInAnyOrder("DASHBOARD", "ORDER");
    }

    @Test
    @DisplayName("Should fall back to role-based resolution when userFeatures is null")
    void testGetAccessibleFeaturesByUserAndTenant_NullUserFeatures() {
        // Given
        User user = User.builder().username("dan").userFeatures(null).build();
        List<String> roleNames = List.of("SHOP_OWNER");
        List<String> roleFeatures = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(tenantContext.getCurrentTenant()).thenReturn(testTenant);
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(eq(roleNames), anyString()))
                .thenReturn(roleFeatures);

        // When
        List<String> result = tenantFeatureService.getAccessibleFeaturesByUserAndTenant(user, roleNames);

        // Then
        assertThat(result).containsExactlyInAnyOrder("DASHBOARD", "ORDER", "CUSTOMER");
    }

    // ============= getShopOwnerRoleNames Tests =============

    @Test
    @DisplayName("Should return persisted SHOP_OWNER role name when role exists")
    void testGetShopOwnerRoleNames_RoleExists() {
        // Given
        Role role = new Role("SHOP_OWNER", "Shop Owner");
        when(roleRepository.findByNameAndTenantId("SHOP_OWNER", "tenant-123"))
                .thenReturn(Optional.of(role));

        // When
        List<String> result = tenantFeatureService.getShopOwnerRoleNames("tenant-123");

        // Then
        assertThat(result).containsExactly("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should fall back to default SHOP_OWNER when role row not yet persisted")
    void testGetShopOwnerRoleNames_RoleMissingFallback() {
        // Given
        when(roleRepository.findByNameAndTenantId("SHOP_OWNER", "new-tenant"))
                .thenReturn(Optional.empty());

        // When
        List<String> result = tenantFeatureService.getShopOwnerRoleNames("new-tenant");

        // Then
        assertThat(result).containsExactly("SHOP_OWNER");
    }

    // ============= Helper =============

    private Feature feature(String name) {
        return Feature.builder().name(name).active(true).build();
    }
}



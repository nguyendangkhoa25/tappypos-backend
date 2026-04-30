package com.knp.service.auth;

import com.knp.model.entity.auth.Feature;
import com.knp.repository.auth.RoleFeatureRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleFeatureService Unit Tests")
class RoleFeatureServiceTest {

    @Mock
    private RoleFeatureRepository roleFeatureRepository;

    @InjectMocks
    private RoleFeatureService roleFeatureService;

    private Feature dashboardFeature;
    private Feature orderFeature;
    private Feature customerFeature;
    private Feature reportFeature;
    private Feature settingsFeature;

    @BeforeEach
    void setUp() {
        // Setup test features
        dashboardFeature = Feature.builder()
                .name("DASHBOARD")
                .displayName("Bảng Điều Khiển")
                .description("Dashboard access")
                .active(true)
                .build();
        dashboardFeature.setId(1L);

        orderFeature = Feature.builder()
                .name("ORDER")
                .displayName("Quản Lý Đơn Hàng")
                .description("Order management")
                .active(true)
                .build();
        orderFeature.setId(2L);

        customerFeature = Feature.builder()
                .name("CUSTOMER")
                .displayName("Quản Lý Khách Hàng")
                .description("Customer management")
                .active(true)
                .build();
        customerFeature.setId(3L);

        reportFeature = Feature.builder()
                .name("REPORT")
                .displayName("Báo Cáo")
                .description("Report generation")
                .active(true)
                .build();
        reportFeature.setId(4L);

        settingsFeature = Feature.builder()
                .name("SETTINGS")
                .displayName("Cài Đặt")
                .description("System settings")
                .active(false)
                .build();
        settingsFeature.setId(5L);
    }

    // ============= getActiveFeaturesByRoleName Tests =============

    @Test
    @DisplayName("Should get active features for a role successfully")
    void testGetActiveFeaturesByRoleName_Success() {
        // Given
        String roleName = "SHOP_OWNER";
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleName(roleName))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleName(roleName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );
        verify(roleFeatureRepository).findActiveFeaturesByRoleName(roleName);
    }

    @Test
    @DisplayName("Should return empty list when role has no active features")
    void testGetActiveFeaturesByRoleName_NoFeatures() {
        // Given
        String roleName = "VIEWER";
        when(roleFeatureRepository.findActiveFeaturesByRoleName(roleName))
                .thenReturn(Collections.emptyList());

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleName(roleName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get features for MANAGER role")
    void testGetActiveFeaturesByRoleName_ManagerRole() {
        // Given
        String roleName = "MANAGER";
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleName(roleName))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleName(roleName);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.getFirst().getName()).isEqualTo("DASHBOARD");
    }

    @Test
    @DisplayName("Should get features for RECEPTIONIST role")
    void testGetActiveFeaturesByRoleName_ReceptionistRole() {
        // Given
        String roleName = "RECEPTIONIST";
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                customerFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleName(roleName))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleName(roleName);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(dashboardFeature, customerFeature);
    }

    // ============= getActiveFeatureNamesByRoleName Tests =============

    @Test
    @DisplayName("Should get active feature names for a role successfully")
    void testGetActiveFeatureNamesByRoleName_Success() {
        // Given
        String roleName = "SHOP_OWNER";
        List<String> expectedFeatureNames = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(roleFeatureRepository.findActiveFeatureNamesByRoleName(roleName))
                .thenReturn(expectedFeatureNames);

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleName(roleName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");
        verify(roleFeatureRepository).findActiveFeatureNamesByRoleName(roleName);
    }

    @Test
    @DisplayName("Should return empty list when role has no feature names")
    void testGetActiveFeatureNamesByRoleName_NoFeatures() {
        // Given
        String roleName = "GUEST";
        when(roleFeatureRepository.findActiveFeatureNamesByRoleName(roleName))
                .thenReturn(Collections.emptyList());

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleName(roleName);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get feature names for MANAGER role")
    void testGetActiveFeatureNamesByRoleName_ManagerRole() {
        // Given
        String roleName = "MANAGER";
        List<String> expectedFeatureNames = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(roleFeatureRepository.findActiveFeatureNamesByRoleName(roleName))
                .thenReturn(expectedFeatureNames);

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleName(roleName);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.getFirst()).isEqualTo("DASHBOARD");
    }

    // ============= getActiveFeaturesByRoleNames Tests =============

    @Test
    @DisplayName("Should get active features for multiple roles successfully")
    void testGetActiveFeaturesByRoleNames_Success() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER", "MANAGER");
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(roleNames);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsAll(expectedFeatures);
        verify(roleFeatureRepository).findActiveFeaturesByRoleNames(roleNames);
    }

    @Test
    @DisplayName("Should return empty list when no roles provided")
    void testGetActiveFeaturesByRoleNames_EmptyRoles() {
        // Given
        List<String> roleNames = Collections.emptyList();
        when(roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames))
                .thenReturn(Collections.emptyList());

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get union of features for multiple roles")
    void testGetActiveFeaturesByRoleNames_MultipleRolesUnion() {
        // Given
        List<String> roleNames = Arrays.asList("MANAGER", "RECEPTIONIST");
        // Union: DASHBOARD, ORDER, CUSTOMER (RECEPTIONIST doesn't add REPORT)
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(roleNames);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains(dashboardFeature, customerFeature);
    }

    @Test
    @DisplayName("Should handle single role in list")
    void testGetActiveFeaturesByRoleNames_SingleRoleInList() {
        // Given
        List<String> roleNames = Collections.singletonList("SHOP_OWNER");
        List<Feature> expectedFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(roleNames);

        // Then
        assertThat(result).hasSize(4);
    }

    // ============= getActiveFeatureNamesByRoleNames Tests =============

    @Test
    @DisplayName("Should get active feature names for multiple roles successfully")
    void testGetActiveFeatureNamesByRoleNames_Success() {
        // Given
        List<String> roleNames = Arrays.asList("SHOP_OWNER", "MANAGER");
        List<String> expectedFeatureNames = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");

        when(roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames))
                .thenReturn(expectedFeatureNames);

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleNames(roleNames);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("DASHBOARD", "ORDER", "CUSTOMER", "REPORT");
        verify(roleFeatureRepository).findActiveFeatureNamesByRoleNames(roleNames);
    }

    @Test
    @DisplayName("Should return empty list when no roles provided")
    void testGetActiveFeatureNamesByRoleNames_EmptyRoles() {
        // Given
        List<String> roleNames = Collections.emptyList();
        when(roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames))
                .thenReturn(Collections.emptyList());

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleNames(roleNames);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get union of feature names for multiple roles")
    void testGetActiveFeatureNamesByRoleNames_MultipleRolesUnion() {
        // Given
        List<String> roleNames = Arrays.asList("MANAGER", "RECEPTIONIST");
        List<String> expectedFeatureNames = Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER");

        when(roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames))
                .thenReturn(expectedFeatureNames);

        // When
        List<String> result = roleFeatureService.getActiveFeatureNamesByRoleNames(roleNames);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains("DASHBOARD", "CUSTOMER");
    }

    // ============= hasRoleAccessToFeature Tests =============

    @Test
    @DisplayName("Should return true when role has access to feature")
    void testHasRoleAccessToFeature_HasAccess() {
        // Given
        String roleName = "SHOP_OWNER";
        String featureName = "ORDER";

        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, featureName))
                .thenReturn(true);

        // When
        boolean result = roleFeatureService.hasRoleAccessToFeature(roleName, featureName);

        // Then
        assertThat(result).isTrue();
        verify(roleFeatureRepository).hasRoleAccessToFeature(roleName, featureName);
    }

    @Test
    @DisplayName("Should return false when role does not have access to feature")
    void testHasRoleAccessToFeature_NoAccess() {
        // Given
        String roleName = "RECEPTIONIST";
        String featureName = "REPORT";

        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, featureName))
                .thenReturn(false);

        // When
        boolean result = roleFeatureService.hasRoleAccessToFeature(roleName, featureName);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should check multiple feature accesses for role")
    void testHasRoleAccessToFeature_MultipleChecks() {
        // Given
        String roleName = "MANAGER";

        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "DASHBOARD"))
                .thenReturn(true);
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "ORDER"))
                .thenReturn(true);
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "CUSTOMER"))
                .thenReturn(true);
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "REPORT"))
                .thenReturn(false);

        // When & Then
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "DASHBOARD")).isTrue();
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "ORDER")).isTrue();
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "CUSTOMER")).isTrue();
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "REPORT")).isFalse();
    }

    @Test
    @DisplayName("Should handle case-sensitive feature names")
    void testHasRoleAccessToFeature_CaseSensitive() {
        // Given
        String roleName = "SHOP_OWNER";

        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "ORDER"))
                .thenReturn(true);
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "order"))
                .thenReturn(false); // Different case

        // When & Then
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "ORDER")).isTrue();
        assertThat(roleFeatureService.hasRoleAccessToFeature(roleName, "order")).isFalse();
    }

    // ============= getAllActiveFeatures Tests =============

    @Test
    @DisplayName("Should get all active features in system")
    void testGetAllActiveFeatures_Success() {
        // Given
        List<Feature> allFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
                // excludes settingsFeature which is inactive
        );

        when(roleFeatureRepository.findAllActiveFeatures())
                .thenReturn(allFeatures);

        // When
        List<Feature> result = roleFeatureService.getAllActiveFeatures();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );
        verify(roleFeatureRepository).findAllActiveFeatures();
    }

    @Test
    @DisplayName("Should return empty list when no active features exist")
    void testGetAllActiveFeatures_NoActiveFeatures() {
        // Given
        when(roleFeatureRepository.findAllActiveFeatures())
                .thenReturn(Collections.emptyList());

        // When
        List<Feature> result = roleFeatureService.getAllActiveFeatures();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not include inactive features in all active features")
    void testGetAllActiveFeatures_ExcludesInactiveFeatures() {
        // Given
        List<Feature> activeFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );

        when(roleFeatureRepository.findAllActiveFeatures())
                .thenReturn(activeFeatures);

        // When
        List<Feature> result = roleFeatureService.getAllActiveFeatures();

        // Then
        assertThat(result)
                .doesNotContain(settingsFeature)
                .hasSize(4);
    }

    @Test
    @DisplayName("Should verify all returned features are active")
    void testGetAllActiveFeatures_VerifyAllActive() {
        // Given
        List<Feature> allFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature,
                reportFeature
        );

        when(roleFeatureRepository.findAllActiveFeatures())
                .thenReturn(allFeatures);

        // When
        List<Feature> result = roleFeatureService.getAllActiveFeatures();

        // Then
        assertThat(result).allMatch(feature -> feature.getActive() != null && feature.getActive());
    }

    // ============= Integration-like Tests =============

    @Test
    @DisplayName("Should handle complete feature access workflow")
    void testCompleteFeatureAccessWorkflow() {
        // Given
        String roleName = "SHOP_OWNER";

        when(roleFeatureRepository.findActiveFeatureNamesByRoleName(roleName))
                .thenReturn(Arrays.asList("DASHBOARD", "ORDER", "CUSTOMER", "REPORT"));
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "DASHBOARD"))
                .thenReturn(true);
        when(roleFeatureRepository.hasRoleAccessToFeature(roleName, "SETTINGS"))
                .thenReturn(false);

        // When
        List<String> features = roleFeatureService.getActiveFeatureNamesByRoleName(roleName);
        boolean dashboardAccess = roleFeatureService.hasRoleAccessToFeature(roleName, "DASHBOARD");
        boolean settingsAccess = roleFeatureService.hasRoleAccessToFeature(roleName, "SETTINGS");

        // Then
        assertThat(features).hasSize(4);
        assertThat(dashboardAccess).isTrue();
        assertThat(settingsAccess).isFalse();
    }

    @Test
    @DisplayName("Should verify feature uniqueness across roles")
    void testFeatureUniquenessAcrossRoles() {
        // Given
        List<String> roleNames = Arrays.asList("MANAGER", "RECEPTIONIST");

        // Setup: MANAGER has DASHBOARD, ORDER, CUSTOMER
        // RECEPTIONIST has DASHBOARD, CUSTOMER
        // Union should be DASHBOARD, ORDER, CUSTOMER (no duplicates)
        List<Feature> unionFeatures = Arrays.asList(
                dashboardFeature,
                orderFeature,
                customerFeature
        );

        when(roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames))
                .thenReturn(unionFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(roleNames);

        // Then
        assertThat(result).hasSize(3);
        // Verify no duplicates
        long uniqueIds = result.stream().map(Feature::getId).distinct().count();
        assertThat(uniqueIds).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle large number of features")
    void testGetAllActiveFeatures_LargeNumberOfFeatures() {
        // Given
        List<Feature> manyFeatures = createFeaturesList(100);

        when(roleFeatureRepository.findAllActiveFeatures())
                .thenReturn(manyFeatures);

        // When
        List<Feature> result = roleFeatureService.getAllActiveFeatures();

        // Then
        assertThat(result).hasSize(100);
        assertThat(result).allMatch(f -> f.getId() != null);
    }

    @Test
    @DisplayName("Should handle large number of roles")
    void testGetActiveFeaturesByRoleNames_LargeNumberOfRoles() {
        // Given
        List<String> manyRoles = Arrays.asList(
                "ROLE1", "ROLE2", "ROLE3", "ROLE4", "ROLE5",
                "ROLE6", "ROLE7", "ROLE8", "ROLE9", "ROLE10"
        );
        List<Feature> expectedFeatures = Arrays.asList(dashboardFeature, orderFeature);

        when(roleFeatureRepository.findActiveFeaturesByRoleNames(manyRoles))
                .thenReturn(expectedFeatures);

        // When
        List<Feature> result = roleFeatureService.getActiveFeaturesByRoleNames(manyRoles);

        // Then
        assertThat(result).hasSize(2);
        verify(roleFeatureRepository).findActiveFeaturesByRoleNames(manyRoles);
    }

    // ============= Helper Methods =============

    @SuppressWarnings("all")
    private List<Feature> createFeaturesList(int count) {
        List<Feature> features = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Feature feature = Feature.builder()
                    .name("FEATURE_" + i)
                    .displayName("Feature " + i)
                    .description("Feature " + i)
                    .active(true)
                    .build();
            feature.setId((long) i);
            features.add(feature);
        }
        return features;
    }
}






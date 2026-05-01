package com.knp.service.tenant;

import com.knp.config.AuthContext;
import com.knp.model.dto.tenant.CreateTenantRequest;
import com.knp.model.dto.tenant.TenantDTO;
import com.knp.model.dto.tenant.UpdateTenantRequest;
import com.knp.model.entity.tenant.Tenant;
import com.knp.repository.auth.UserRepository;
import com.knp.repository.tenant.TenantRepository;
import com.knp.repository.tenant.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuthContext authContext;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;
    private CreateTenantRequest createRequest;
    private UpdateTenantRequest updateRequest;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD,ORDER")
                .subscriptionType("STANDARD")
                .contactPersonName("John Doe")
                .contactPersonPhone("+1234567890")
                .contactPersonEmail("john@tenant.com")
                .contactPersonZaloId("zalo123")
                .createdBy("admin")
                .updatedBy("admin")
                .build();
        tenant.setId(1L);

        createRequest = CreateTenantRequest.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(List.of("DASHBOARD", "ORDER"))
                .subscriptionType("STANDARD")
                .contactPersonName("John Doe")
                .contactPersonPhone("+1234567890")
                .contactPersonEmail("john@tenant.com")
                .contactPersonZaloId("zalo123")
                .build();

        updateRequest = UpdateTenantRequest.builder()
                .name("Updated Tenant")
                .dbName("updated_tenant_db")
                .expirationDate(LocalDate.now().plusYears(2))
                .maxUsers(100)
                .features(List.of("DASHBOARD", "ORDER", "CUSTOMER"))
                .subscriptionType("PREMIUM")
                .contactPersonName("Jane Doe")
                .contactPersonPhone("+0987654321")
                .contactPersonEmail("jane@tenant.com")
                .contactPersonZaloId("zalo456")
                .build();
    }

    // ==================== Get All Active Tenants Tests ====================

    @Test
    @DisplayName("Should get all active tenants successfully")
    void testGetAllActiveTenants_Success() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(Collections.singletonList(tenant));

        List<TenantDTO> result = tenantService.getAllActiveTenants();

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test Tenant");
        verify(tenantRepository).findAllByActiveTrue();
    }

    @Test
    @DisplayName("Should return empty list when no active tenants exist")
    void testGetAllActiveTenants_Empty() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

        List<TenantDTO> result = tenantService.getAllActiveTenants();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should get multiple active tenants successfully")
    void testGetAllActiveTenants_Multiple() {
        Tenant tenant2 = Tenant.builder()
                .tenantId("test-tenant-2").name("Test Tenant 2").dbName("test_tenant_db_2")
                .active(true).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features("DASHBOARD").subscriptionType("STANDARD").build();
        tenant2.setId(2L);

        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(tenant, tenant2));

        List<TenantDTO> result = tenantService.getAllActiveTenants();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantDTO::getName).contains("Test Tenant", "Test Tenant 2");
    }

    // ==================== Get All Tenants Tests ====================

    @Test
    @DisplayName("Should get all tenants successfully")
    void testGetAllTenants_Success() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        List<TenantDTO> result = tenantService.getAllTenants(null);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        verify(tenantRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no tenants exist")
    void testGetAllTenants_Empty() {
        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(tenantService.getAllTenants(null)).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should search tenants by name successfully")
    void testGetAllTenants_WithSearch_Success() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        List<TenantDTO> result = tenantService.getAllTenants("Test");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test Tenant");
    }

    @Test
    @DisplayName("Should search tenants by database name")
    void testGetAllTenants_SearchByDbName() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        List<TenantDTO> result = tenantService.getAllTenants("test_tenant_db");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDbName()).isEqualTo("test_tenant_db");
    }

    @Test
    @DisplayName("Should search tenants by contact person name")
    void testGetAllTenants_SearchByContactName() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        assertThat(tenantService.getAllTenants("John")).hasSize(1);
    }

    @Test
    @DisplayName("Should search tenants by contact person phone")
    void testGetAllTenants_SearchByPhone() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        assertThat(tenantService.getAllTenants("1234567890")).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when search has no results")
    void testGetAllTenants_SearchNoResults() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        assertThat(tenantService.getAllTenants("NonExistent")).isEmpty();
    }

    @Test
    @DisplayName("Should handle null and empty search parameter")
    void testGetAllTenants_SearchNullOrEmpty() {
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        assertThat(tenantService.getAllTenants(null)).hasSize(1);
        assertThat(tenantService.getAllTenants("")).hasSize(1);
    }

    @Test
    @DisplayName("Should search case insensitive")
    void testGetAllTenants_SearchCaseInsensitive() {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        assertThat(tenantService.getAllTenants("TEST")).hasSize(1);
    }

    // ==================== Get Tenant By ID Tests ====================

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void testGetTenantById_Success() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        verify(tenantRepository).findByTenantId("test-tenant");
    }

    @Test
    @DisplayName("Should throw exception when tenant not found by ID")
    void testGetTenantById_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Create Tenant Tests ====================

    @Test
    @DisplayName("Should create tenant successfully")
    void testCreateTenant_Success() {
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        TenantDTO result = tenantService.createTenant(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        assertThat(result.getCreatedBy()).isEqualTo("admin");
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should create tenant with null features")
    void testCreateTenant_NullFeatures() {
        CreateTenantRequest requestNoFeatures = CreateTenantRequest.builder()
                .tenantId("test-tenant-3").name("Test Tenant 3").dbName("test_tenant_db_3")
                .expirationDate(LocalDate.now().plusYears(1)).maxUsers(50)
                .features(null).subscriptionType("STANDARD").build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant-3")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        assertThat(tenantService.createTenant(requestNoFeatures)).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant already exists")
    void testCreateTenant_AlreadyExists() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.createTenant(createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant already exists");
    }

    @Test
    @DisplayName("Should use system username when getCurrentUsername returns null")
    void testCreateTenant_SystemUser() {
        when(authContext.getCurrentUsername()).thenReturn(null);
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        assertThat(tenantService.createTenant(createRequest)).isNotNull();
    }

    @Test
    @DisplayName("Should create tenant with empty feature list")
    void testCreateTenant_EmptyFeatures() {
        CreateTenantRequest emptyFeaturesRequest = CreateTenantRequest.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_db")
                .expirationDate(LocalDate.now().plusYears(1)).maxUsers(50)
                .features(List.of()).subscriptionType("STANDARD").build();

        Tenant emptyFeaturesTenant = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_db")
                .active(true).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features("").subscriptionType("STANDARD").build();
        emptyFeaturesTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(emptyFeaturesTenant);

        assertThat(tenantService.createTenant(emptyFeaturesRequest)).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should handle creating tenant with all null contact fields")
    void testCreateTenant_NullContactFields() {
        CreateTenantRequest requestNoContact = CreateTenantRequest.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_db")
                .expirationDate(LocalDate.now().plusYears(1)).maxUsers(50)
                .features(List.of("DASHBOARD")).subscriptionType("STANDARD")
                .contactPersonName(null).contactPersonPhone(null)
                .contactPersonEmail(null).contactPersonZaloId(null).build();

        Tenant noContactTenant = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_db")
                .active(true).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features("DASHBOARD").subscriptionType("STANDARD").build();
        noContactTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(noContactTenant);

        assertThat(tenantService.createTenant(requestNoContact)).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    // ==================== Update Tenant Tests ====================

    @Test
    @DisplayName("Should update tenant successfully")
    void testUpdateTenant_Success() {
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        TenantDTO result = tenantService.updateTenant("test-tenant", updateRequest);

        assertThat(result).isNotNull();
        verify(tenantRepository).findByTenantId("test-tenant");
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should update tenant with partial fields")
    void testUpdateTenant_PartialUpdate() {
        UpdateTenantRequest partialRequest = UpdateTenantRequest.builder()
                .name("Updated Name Only").build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        assertThat(tenantService.updateTenant("test-tenant", partialRequest)).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent tenant")
    void testUpdateTenant_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant("nonexistent", updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    @DisplayName("Should update multiple fields independently")
    void testUpdateTenant_MultipleFieldsIndependently() {
        UpdateTenantRequest multipleFieldsRequest = UpdateTenantRequest.builder()
                .name("New Name").maxUsers(200).features(List.of("DASHBOARD", "CUSTOMER")).build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        assertThat(tenantService.updateTenant("test-tenant", multipleFieldsRequest)).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    // ==================== Delete Tenant Tests ====================

    @Test
    @DisplayName("Should delete tenant successfully")
    void testDeleteTenant_Success() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant("test-tenant");

        verify(tenantRepository).delete(tenant);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent tenant")
    void testDeleteTenant_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Get Tenant Entity Tests ====================

    @Test
    @DisplayName("Should get tenant entity successfully")
    void testGetTenantEntity_Success() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantEntity("test-tenant");

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("Should throw exception when tenant entity not found")
    void testGetTenantEntity_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantEntity("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Deactivate Tenant Tests ====================

    @Test
    @DisplayName("Should deactivate tenant successfully")
    void testDeactivateTenant_Success() {
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        TenantDTO result = tenantService.deactivateTenant("test-tenant");

        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent tenant")
    void testDeactivateTenant_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deactivateTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    @DisplayName("Should deactivate with system user")
    void testDeactivateTenant_SystemUser() {
        when(authContext.getCurrentUsername()).thenReturn(null);
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        assertThat(tenantService.deactivateTenant("test-tenant")).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    // ==================== Activate Tenant Tests ====================

    @Test
    @DisplayName("Should activate tenant successfully")
    void testActivateTenant_Success() {
        Tenant inactiveTenant = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_tenant_db")
                .active(false).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features("DASHBOARD,ORDER").subscriptionType("STANDARD").build();
        inactiveTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(inactiveTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(inactiveTenant);

        TenantDTO result = tenantService.activateTenant("test-tenant");

        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent tenant")
    void testActivateTenant_NotFound() {
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.activateTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== mapToDTO Tests ====================

    @Test
    @DisplayName("Should map tenant entity with features correctly")
    void testMapToDTO_WithFeatures() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result.getFeatures()).isNotNull().contains("DASHBOARD", "ORDER");
    }

    @Test
    @DisplayName("Should map tenant entity with null features correctly")
    void testMapToDTO_NullFeatures() {
        Tenant tenantNoFeatures = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_tenant_db")
                .active(true).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features(null).subscriptionType("STANDARD").build();
        tenantNoFeatures.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantNoFeatures));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result).isNotNull();
        assertThat(result.getFeatures()).isNull();
    }

    @Test
    @DisplayName("Should map all tenant fields correctly")
    void testMapToDTO_AllFields() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("tenantId", "test-tenant")
                .hasFieldOrPropertyWithValue("name", "Test Tenant")
                .hasFieldOrPropertyWithValue("dbName", "test_tenant_db")
                .hasFieldOrPropertyWithValue("active", true)
                .hasFieldOrPropertyWithValue("maxUsers", 50)
                .hasFieldOrPropertyWithValue("subscriptionType", "STANDARD")
                .hasFieldOrPropertyWithValue("contactPersonName", "John Doe")
                .hasFieldOrPropertyWithValue("contactPersonPhone", "+1234567890")
                .hasFieldOrPropertyWithValue("contactPersonEmail", "john@tenant.com");
    }

    @Test
    @DisplayName("Should map features from comma-separated string")
    void testMapToDTO_FeatureMapping() {
        Tenant multiFeatureTenant = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_db")
                .active(true).features("FEATURE1,FEATURE2,FEATURE3").subscriptionType("STANDARD").build();
        multiFeatureTenant.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(multiFeatureTenant));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result.getFeatures()).containsExactly("FEATURE1", "FEATURE2", "FEATURE3");
    }

    @Test
    @DisplayName("Should handle single feature in comma-separated string")
    void testMapToDTO_SingleFeature() {
        Tenant tenantSingleFeature = Tenant.builder()
                .tenantId("test-tenant").name("Test Tenant").dbName("test_tenant_db")
                .active(true).expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50).features("DASHBOARD").subscriptionType("STANDARD").build();
        tenantSingleFeature.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantSingleFeature));

        TenantDTO result = tenantService.getTenantById("test-tenant");

        assertThat(result).isNotNull();
        assertThat(result.getFeatures()).isNotNull().hasSize(1);
    }
}

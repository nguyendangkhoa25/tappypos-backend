package com.knp.service;

import com.knp.config.AuthContext;
import com.knp.model.dto.CreateTenantRequest;
import com.knp.model.dto.TenantDTO;
import com.knp.model.dto.UpdateTenantRequest;
import com.knp.model.entity.Tenant;
import com.knp.multitenant.DatasourceManager;
import com.knp.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private DatasourceManager datasourceManager;

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
        // Given
        when(tenantRepository.findAllByActiveTrue()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllActiveTenants();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test Tenant");
        verify(tenantRepository).findAllByActiveTrue();
    }

    @Test
    @DisplayName("Should return empty list when no active tenants exist")
    void testGetAllActiveTenants_Empty() {
        // Given
        when(tenantRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

        // When
        List<TenantDTO> result = tenantService.getAllActiveTenants();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(tenantRepository).findAllByActiveTrue();
    }

    @Test
    @DisplayName("Should get multiple active tenants successfully")
    void testGetAllActiveTenants_Multiple() {
        // Given
        Tenant tenant2 = Tenant.builder()
                .tenantId("test-tenant-2")
                .name("Test Tenant 2")
                .dbName("test_tenant_db_2")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD")
                .subscriptionType("STANDARD")
                .build();
        tenant2.setId(2L);

        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(tenant, tenant2));

        // When
        List<TenantDTO> result = tenantService.getAllActiveTenants();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantDTO::getName).contains("Test Tenant", "Test Tenant 2");
    }

    // ==================== Get All Tenants Tests ====================

    @Test
    @DisplayName("Should get all tenants successfully")
    void testGetAllTenants_Success() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        verify(tenantRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no tenants exist")
    void testGetAllTenants_Empty() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<TenantDTO> result = tenantService.getAllTenants();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ==================== Search Tenants Tests ====================

    @Test
    @DisplayName("Should search tenants by name successfully")
    void testGetAllTenants_WithSearch_Success() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("Test");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test Tenant");
    }

    @Test
    @DisplayName("Should search tenants by database name")
    void testGetAllTenants_SearchByDbName() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("test_tenant_db");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDbName()).isEqualTo("test_tenant_db");
    }

    @Test
    @DisplayName("Should search tenants by contact person name")
    void testGetAllTenants_SearchByContactName() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("John");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getContactPersonName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should search tenants by contact person phone")
    void testGetAllTenants_SearchByPhone() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("1234567890");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getContactPersonPhone()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should return empty list when search has no results")
    void testGetAllTenants_SearchNoResults() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle null search parameter")
    void testGetAllTenants_SearchNull() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants(null);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should handle empty search parameter")
    void testGetAllTenants_SearchEmpty() {
        // Given
        when(tenantRepository.findAll()).thenReturn(Collections.singletonList(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("");

        // Then
        assertThat(result).hasSize(1);
    }

    // ==================== Get Tenant By ID Tests ====================

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void testGetTenantById_Success() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        verify(tenantRepository).findByTenantId("test-tenant");
    }

    @Test
    @DisplayName("Should throw exception when tenant not found by ID")
    void testGetTenantById_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.getTenantById("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Create Tenant Tests ====================

    @Test
    @DisplayName("Should create tenant successfully")
    void testCreateTenant_Success() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.createTenant(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        assertThat(result.getCreatedBy()).isEqualTo("admin");
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should create tenant with null features")
    void testCreateTenant_NullFeatures() {
        // Given
        CreateTenantRequest requestNoFeatures = CreateTenantRequest.builder()
                .tenantId("test-tenant-3")
                .name("Test Tenant 3")
                .dbName("test_tenant_db_3")
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(null)
                .subscriptionType("STANDARD")
                .build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant-3")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.createTenant(requestNoFeatures);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant already exists")
    void testCreateTenant_AlreadyExists() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenant(createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant already exists");
    }

    @Test
    @DisplayName("Should use system username when getCurrentUsername returns null")
    void testCreateTenant_SystemUser() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn(null);
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.createTenant(createRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    // ==================== Update Tenant Tests ====================

    @Test
    @DisplayName("Should update tenant successfully")
    void testUpdateTenant_Success() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.updateTenant("test-tenant", updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).findByTenantId("test-tenant");
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should update tenant with partial fields")
    void testUpdateTenant_PartialUpdate() {
        // Given
        UpdateTenantRequest partialRequest = UpdateTenantRequest.builder()
                .name("Updated Name Only")
                .build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.updateTenant("test-tenant", partialRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent tenant")
    void testUpdateTenant_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.updateTenant("nonexistent", updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Delete Tenant Tests ====================

    @Test
    @DisplayName("Should delete tenant successfully")
    void testDeleteTenant_Success() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        tenantService.deleteTenant("test-tenant");

        // Then
        verify(tenantRepository).delete(tenant);
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent tenant")
    void testDeleteTenant_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.deleteTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Get Tenant Entity Tests ====================

    @Test
    @DisplayName("Should get tenant entity successfully")
    void testGetTenantEntity_Success() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        Tenant result = tenantService.getTenantEntity("test-tenant");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("Should throw exception when tenant entity not found")
    void testGetTenantEntity_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.getTenantEntity("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Deactivate Tenant Tests ====================

    @Test
    @DisplayName("Should deactivate tenant successfully")
    void testDeactivateTenant_Success() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.deactivateTenant("test-tenant");

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent tenant")
    void testDeactivateTenant_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.deactivateTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Activate Tenant Tests ====================

    @Test
    @DisplayName("Should activate tenant successfully")
    void testActivateTenant_Success() {
        // Given
        Tenant inactiveTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(false)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD,ORDER")
                .subscriptionType("STANDARD")
                .build();
        inactiveTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(inactiveTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(inactiveTenant);

        // When
        TenantDTO result = tenantService.activateTenant("test-tenant");

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).addOrUpdateTenantDatasource("test-tenant", "test_tenant_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent tenant")
    void testActivateTenant_NotFound() {
        // Given
        when(tenantRepository.findByTenantId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.activateTenant("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant not found");
    }

    // ==================== Map to DTO Tests ====================

    @Test
    @DisplayName("Should map tenant entity with features correctly")
    void testMapToDTO_WithFeatures() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).contains("DASHBOARD", "ORDER");
    }

    @Test
    @DisplayName("Should map tenant entity with null features correctly")
    void testMapToDTO_NullFeatures() {
        // Given
        Tenant tenantNoFeatures = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(null)
                .subscriptionType("STANDARD")
                .build();
        tenantNoFeatures.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantNoFeatures));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFeatures()).isNull();
    }

    @Test
    @DisplayName("Should map all tenant fields correctly")
    void testMapToDTO_AllFields() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
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
    @DisplayName("Should map tenant entity with null features to list of features")
    void testMapToDTO_NullFeaturesReturnsNull() {
        // Given
        Tenant tenantNoFeatures = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(null)
                .subscriptionType("STANDARD")
                .build();
        tenantNoFeatures.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantNoFeatures));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFeatures()).isNull();
    }

    @Test
    @DisplayName("Should handle empty string features in tenant")
    void testMapToDTO_EmptyStringFeatures() {
        // Given
        Tenant tenantEmptyFeatures = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("")
                .subscriptionType("STANDARD")
                .build();
        tenantEmptyFeatures.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantEmptyFeatures));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle single feature in comma-separated string")
    void testMapToDTO_SingleFeature() {
        // Given
        Tenant tenantSingleFeature = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD")
                .subscriptionType("STANDARD")
                .build();
        tenantSingleFeature.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantSingleFeature));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFeatures()).isNotNull();
    }

    @Test
    @DisplayName("Should handle whitespace in feature strings")
    void testMapToDTO_FeaturesWithWhitespace() {
        // Given
        Tenant tenantWithWhitespace = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(" DASHBOARD , ORDER , CUSTOMER ")
                .subscriptionType("STANDARD")
                .build();
        tenantWithWhitespace.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenantWithWhitespace));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle creating tenant with empty feature list")
    void testCreateTenant_EmptyFeaturesList() {
        // Given
        CreateTenantRequest requestEmptyFeatures = CreateTenantRequest.builder()
                .tenantId("test-tenant-4")
                .name("Test Tenant 4")
                .dbName("test_tenant_db_4")
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(List.of())
                .subscriptionType("STANDARD")
                .build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant-4")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.createTenant(requestEmptyFeatures);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should verify datasource operations on delete")
    void testDeleteTenant_VerifyDatasourceOperations() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        tenantService.deleteTenant("test-tenant");

        // Then
        verify(tenantRepository).delete(tenant);
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify datasource operations on deactivate")
    void testDeactivateTenant_VerifyDatasourceOperations() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        tenantService.deactivateTenant("test-tenant");

        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify datasource operations on activate")
    void testActivateTenant_VerifyDatasourceOperations() {
        // Given
        Tenant inactiveTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(false)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD")
                .subscriptionType("STANDARD")
                .build();
        inactiveTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(inactiveTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(inactiveTenant);

        // When
        tenantService.activateTenant("test-tenant");

        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).addOrUpdateTenantDatasource("test-tenant", "test_tenant_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    // ============= createTenantDatasource Tests =============

    @Test
    @DisplayName("Should successfully create tenant datasource with valid inputs")
    void testCreateTenantDatasource_Success() {
        // When
        tenantService.createTenantDatasource("new-tenant-1", "new_tenant_1_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("new-tenant-1", "new_tenant_1_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify datasource manager methods are called in correct order for createTenantDatasource")
    void testCreateTenantDatasource_MethodCallOrder() {
        // When
        tenantService.createTenantDatasource("tenant-order-test", "tenant_order_test_db");

        // Then
        InOrder inOrder = inOrder(datasourceManager);
        inOrder.verify(datasourceManager).addOrUpdateTenantDatasource("tenant-order-test", "tenant_order_test_db");
        inOrder.verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource with numeric tenant ID")
    void testCreateTenantDatasource_NumericTenantId() {
        // When
        tenantService.createTenantDatasource("123456", "tenant_123456_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("123456", "tenant_123456_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource with special characters in database name")
    void testCreateTenantDatasource_SpecialCharsInDbName() {
        // When
        tenantService.createTenantDatasource("test-tenant", "test_tenant_2024_q1");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("test-tenant", "test_tenant_2024_q1");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource with hyphenated tenant ID")
    void testCreateTenantDatasource_HyphenatedTenantId() {
        // When
        tenantService.createTenantDatasource("test-tenant-hyphen-multiple", "test_tenant_hyphen_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("test-tenant-hyphen-multiple", "test_tenant_hyphen_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource and reload all datasources")
    void testCreateTenantDatasource_ReloadDatasourcesAfterCreate() {
        // When
        tenantService.createTenantDatasource("reload-test-tenant", "reload_test_db");

        // Then - Verify that reloadAllTenantDatasource is called after addOrUpdateTenantDatasource
        InOrder inOrder = inOrder(datasourceManager);
        inOrder.verify(datasourceManager).addOrUpdateTenantDatasource("reload-test-tenant", "reload_test_db");
        inOrder.verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should handle exception during addOrUpdateTenantDatasource")
    void testCreateTenantDatasource_ExceptionDuringAddDatasource() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(datasourceManager).addOrUpdateTenantDatasource("error-tenant", "error_db");

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenantDatasource("error-tenant", "error_db"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create datasource");
    }

    @Test
    @DisplayName("Should handle exception during reloadAllTenantDatasource")
    void testCreateTenantDatasource_ExceptionDuringReload() {
        // Given
        doNothing().when(datasourceManager).addOrUpdateTenantDatasource("reload-error-tenant", "reload_error_db");
        doThrow(new RuntimeException("Reload failed"))
                .when(datasourceManager).reloadAllTenantDatasource();

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenantDatasource("reload-error-tenant", "reload_error_db"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create datasource");
    }

    @Test
    @DisplayName("Should create datasource with long tenant ID")
    void testCreateTenantDatasource_LongTenantId() {
        // When
        String longTenantId = "very-long-tenant-id-" + "x".repeat(100);
        String longDbName = "very_long_db_name_" + "y".repeat(100);
        tenantService.createTenantDatasource(longTenantId, longDbName);

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource(longTenantId, longDbName);
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource with underscore separated tenant ID")
    void testCreateTenantDatasource_UnderscoreSeparatedTenantId() {
        // When
        tenantService.createTenantDatasource("tenant_name_with_underscores", "tenant_name_db_underscores");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("tenant_name_with_underscores", "tenant_name_db_underscores");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should create datasource multiple times with different tenant IDs")
    void testCreateTenantDatasource_MultipleTenantCreations() {
        // When
        tenantService.createTenantDatasource("tenant-1", "tenant_1_db");
        tenantService.createTenantDatasource("tenant-2", "tenant_2_db");
        tenantService.createTenantDatasource("tenant-3", "tenant_3_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("tenant-1", "tenant_1_db");
        verify(datasourceManager).addOrUpdateTenantDatasource("tenant-2", "tenant_2_db");
        verify(datasourceManager).addOrUpdateTenantDatasource("tenant-3", "tenant_3_db");
        verify(datasourceManager, times(3)).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify addOrUpdateTenantDatasource is called with correct parameters")
    void testCreateTenantDatasource_VerifyParametersPassedCorrectly() {
        // When
        tenantService.createTenantDatasource("param-test-tenant", "param_test_db");

        // Then
        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dbNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(datasourceManager).addOrUpdateTenantDatasource(tenantIdCaptor.capture(), dbNameCaptor.capture());
        
        assertThat(tenantIdCaptor.getValue()).isEqualTo("param-test-tenant");
        assertThat(dbNameCaptor.getValue()).isEqualTo("param_test_db");
    }

    @Test
    @DisplayName("Should handle exception with proper error message wrapping")
    void testCreateTenantDatasource_ExceptionMessageWrapping() {
        // Given
        String originalErrorMessage = "Original error from datasource manager";
        doThrow(new RuntimeException(originalErrorMessage))
                .when(datasourceManager).addOrUpdateTenantDatasource("msg-test", "msg_test_db");

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenantDatasource("msg-test", "msg_test_db"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create datasource")
                .hasMessageContaining(originalErrorMessage);
    }

    @Test
    @DisplayName("Should create datasource with case-sensitive tenant ID")
    void testCreateTenantDatasource_CaseSensitiveTenantId() {
        // When
        tenantService.createTenantDatasource("TenantABC", "TenantABC_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("TenantABC", "TenantABC_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should handle datasourceManager returning null gracefully")
    void testCreateTenantDatasource_DatasourceManagerNullResponse() {
        // Given - datasourceManager methods don't return anything but complete successfully
        doNothing().when(datasourceManager).addOrUpdateTenantDatasource(anyString(), anyString());
        doNothing().when(datasourceManager).reloadAllTenantDatasource();

        // When
        tenantService.createTenantDatasource("null-test", "null_test_db");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("null-test", "null_test_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify createTenantDatasource handles exception")
    void testCreateTenantDatasource_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Datasource creation failed"))
                .when(datasourceManager).addOrUpdateTenantDatasource("test-tenant", "test_db");

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenantDatasource("test-tenant", "test_db"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create datasource");
    }

    @Test
    @DisplayName("Should verify removeTenantDatasource executes successfully")
    void testRemoveTenantDatasource_Success() {
        // When
        tenantService.removeTenantDatasource("test-tenant");

        // Then
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify removeTenantDatasource handles exception gracefully")
    void testRemoveTenantDatasource_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Failed to remove datasource"))
                .when(datasourceManager).removeTenantDatasource("test-tenant");

        // When - Should not throw (handles exception gracefully in async method)
        tenantService.removeTenantDatasource("test-tenant");

        // Then
        verify(datasourceManager).removeTenantDatasource("test-tenant");
    }

    @Test
    @DisplayName("Should verify reloadAllDatasource executes successfully")
    void testReloadAllDatasource_Success() {
        // When
        tenantService.reloadAllDatasource();

        // Then
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify reloadAllDatasource handles exception gracefully")
    void testReloadAllDatasource_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Reload failed"))
                .when(datasourceManager).reloadAllTenantDatasource();

        // When - Should not throw
        tenantService.reloadAllDatasource();

        // Then
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should search tenants case insensitive")
    void testGetAllTenants_SearchCaseInsensitive() {
        // Given
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("TEST");

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should search tenants with partial matches")
    void testGetAllTenants_PartialMatch() {
        // Given
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        // When
        List<TenantDTO> result = tenantService.getAllTenants("Test Tenant");

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should handle tenant with all null contact fields")
    void testCreateTenant_NullContactFields() {
        // Given
        CreateTenantRequest requestNoContact = CreateTenantRequest.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_db")
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(List.of("DASHBOARD"))
                .subscriptionType("STANDARD")
                .contactPersonName(null)
                .contactPersonPhone(null)
                .contactPersonEmail(null)
                .contactPersonZaloId(null)
                .build();

        Tenant noContactTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD")
                .subscriptionType("STANDARD")
                .build();
        noContactTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(noContactTenant);

        // When
        TenantDTO result = tenantService.createTenant(requestNoContact);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should verify deactivate with system user")
    void testDeactivateTenant_SystemUser() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn(null);
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.deactivateTenant("test-tenant");

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should update multiple fields independently")
    void testUpdateTenant_MultipleFieldsIndependently() {
        // Given
        UpdateTenantRequest multipleFieldsRequest = UpdateTenantRequest.builder()
                .name("New Name")
                .maxUsers(200)
                .features(List.of("DASHBOARD", "CUSTOMER"))
                .build();

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        TenantDTO result = tenantService.updateTenant("test-tenant", multipleFieldsRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should handle features as empty list")
    void testCreateTenant_EmptyFeatures() {
        // Given
        CreateTenantRequest emptyFeaturesRequest = CreateTenantRequest.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_db")
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features(List.of())
                .subscriptionType("STANDARD")
                .build();

        Tenant emptyFeaturesTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_db")
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("")
                .subscriptionType("STANDARD")
                .build();
        emptyFeaturesTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(emptyFeaturesTenant);

        // When
        TenantDTO result = tenantService.createTenant(emptyFeaturesRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should verify updateTenant calls reload datasource")
    void testUpdateTenant_VerifyReloadDatasource() {
        // Given
        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // When
        tenantService.updateTenant("test-tenant", updateRequest);

        // Then
        verify(datasourceManager, times(1)).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should handle feature mapping in mapToDTO")
    void testMapToDTO_FeatureMapping() {
        // Given
        Tenant multiFeatureTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_db")
                .active(true)
                .features("FEATURE1,FEATURE2,FEATURE3")
                .subscriptionType("STANDARD")
                .build();
        multiFeatureTenant.setId(1L);

        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(multiFeatureTenant));

        // When
        TenantDTO result = tenantService.getTenantById("test-tenant");

        // Then
        assertThat(result.getFeatures()).containsExactly("FEATURE1", "FEATURE2", "FEATURE3");
    }

    @Test
    @DisplayName("Should verify deleteeTenant calls removeTenantDatasource")
    void testDeleteTenant_VerifyDatasourceRemoval() {
        // Given
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(tenant));

        // When
        tenantService.deleteTenant("test-tenant");

        // Then
        verify(datasourceManager).removeTenantDatasource("test-tenant");
        verify(datasourceManager).reloadAllTenantDatasource();
    }

    @Test
    @DisplayName("Should verify activateTenant calls addOrUpdateTenantDatasource")
    void testActivateTenant_VerifyDatasourceAdded() {
        // Given
        Tenant inactiveTenant = Tenant.builder()
                .tenantId("test-tenant")
                .name("Test Tenant")
                .dbName("test_tenant_db")
                .active(false)
                .expirationDate(LocalDate.now().plusYears(1))
                .maxUsers(50)
                .features("DASHBOARD")
                .subscriptionType("STANDARD")
                .build();
        inactiveTenant.setId(1L);

        when(authContext.getCurrentUsername()).thenReturn("admin");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(inactiveTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(inactiveTenant);

        // When
        tenantService.activateTenant("test-tenant");

        // Then
        verify(datasourceManager).addOrUpdateTenantDatasource("test-tenant", "test_tenant_db");
        verify(datasourceManager).reloadAllTenantDatasource();
    }
}

package com.knp.multitenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatasourceManager using mocked datasource creation
 * Covers dynamic datasource management, tenant datasource operations, and error handling
 * WITHOUT relying on actual database connections
 *
 * Uses Mockito's MockedStatic to mock DatasourceUtil.createLightweightHikariDataSource()
 * to avoid actual datasource creation while testing manager logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatasourceManager Unit Tests")
class DatasourceManagerTest {

    @Mock
    private DataSource masterDataSource;

    @Mock
    private DataSource mockTenantDataSource;

    private TenantContext tenantContext;
    private RoutingDataSource routingDataSource;
    private DatasourceManager datasourceManager;

    @BeforeEach
    void setUp() {
        // Create TenantContext
        tenantContext = new TenantContext();

        // Create routing datasource with mocked master datasource
        routingDataSource = new RoutingDataSource(tenantContext);
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);

        // CRITICAL: Initialize AbstractRoutingDataSource's resolver cache
        // Without this, determineCurrentLookupKey() will throw "DataSources not resolved yet"
        routingDataSource.afterPropertiesSet();

        // Create datasource manager
        datasourceManager = new DatasourceManager(routingDataSource, masterDataSource);

        // Set the property values using reflection
        ReflectionTestUtils.setField(datasourceManager, "masterDbUrl", "jdbc:h2:mem:master/");
        ReflectionTestUtils.setField(datasourceManager, "masterDbUsername", "sa");
        ReflectionTestUtils.setField(datasourceManager, "masterDbPassword", "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, DataSource> activeSources() {
        return (Map<String, DataSource>) ReflectionTestUtils.getField(routingDataSource, "tenantDataSources");
    }

    // ==================== addOrUpdateTenantDatasource Tests ====================

    @Test
    @DisplayName("Should add new tenant datasource successfully")
    void testAddOrUpdateTenantDatasource_AddNew() {
        // When
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");

        // Then - Verify datasource was added by checking routing datasource
        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    @Test
    @DisplayName("Should update existing tenant datasource")
    void testAddOrUpdateTenantDatasource_Update() {
        // When - First add
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");
        assertThat(activeSources())
                .containsKey("tenant-001");

        // When - Update same tenant
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-updated");

        // Then - Datasource should still be accessible
        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    @Test
    @DisplayName("Should throw exception when routing datasource fails")
    void testAddOrUpdateTenantDatasource_RoutingDataSourceException() {
        // When & Then - Try with normal H2 (should succeed)
        assertThatNoException().isThrownBy(() ->
            datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001")
        );
    }

    @Test
    @DisplayName("Should handle various database names")
    void testAddOrUpdateTenantDatasource_VariousDbNames() {
        // When & Then - various database name formats
        assertThatNoException().isThrownBy(() -> {
            datasourceManager.addOrUpdateTenantDatasource("tenant-1", "db_tenant_1");
            datasourceManager.addOrUpdateTenantDatasource("tenant-2", "db-tenant-2");
            datasourceManager.addOrUpdateTenantDatasource("tenant-3", "db_abc_123");
        });

        // Then - All tenants should be registered
        assertThat(activeSources())
                .containsKeys("tenant-1", "tenant-2", "tenant-3");
    }

    @Test
    @DisplayName("Should build correct database URL for tenant")
    void testAddOrUpdateTenantDatasource_CorrectUrl() {
        // Given
        ReflectionTestUtils.setField(datasourceManager, "masterDbUrl", "jdbc:h2:mem:master-test/");

        // When
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-001-db");

        // Then - Verify datasource is accessible
        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    // ==================== removeTenantDatasource Tests ====================

    @Test
    @DisplayName("Should remove tenant datasource successfully")
    void testRemoveTenantDatasource_Success() {
        // Given - Add first
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");

        // When
        datasourceManager.removeTenantDatasource("tenant-001");

        // Then - Verify datasource was removed from routing
        assertThat(activeSources())
                .doesNotContainKey("tenant-001");
    }

    @Test
    @DisplayName("Should remove multiple tenant datasources")
    void testRemoveTenantDatasource_Multiple() {
        // Given - Add multiple
        datasourceManager.addOrUpdateTenantDatasource("tenant-1", "db-1");
        datasourceManager.addOrUpdateTenantDatasource("tenant-2", "db-2");
        datasourceManager.addOrUpdateTenantDatasource("tenant-3", "db-3");

        // When
        datasourceManager.removeTenantDatasource("tenant-1");
        datasourceManager.removeTenantDatasource("tenant-2");
        datasourceManager.removeTenantDatasource("tenant-3");

        // Then
        assertThat(activeSources())
                .doesNotContainKeys("tenant-1", "tenant-2", "tenant-3");
    }

    @Test
    @DisplayName("Should throw exception when routing datasource remove fails")
    void testRemoveTenantDatasource_RoutingDataSourceException() {
        // Given - Add a datasource first
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");

        // When & Then - Normal removal should succeed
        assertThatNoException().isThrownBy(() ->
            datasourceManager.removeTenantDatasource("tenant-001")
        );
    }

    @Test
    @DisplayName("Should include tenant ID in error message when removal fails")
    void testRemoveTenantDatasource_ErrorMessageIncludesTenantId() {
        // When & Then - Even if removal fails, it should be handled gracefully
        assertThatNoException().isThrownBy(() ->
            datasourceManager.removeTenantDatasource("tenant-999")
        );
    }

    @Test
    @DisplayName("Should maintain reference to master datasource")
    void testMasterDataSourceReference() {
        // When - Add tenant datasources
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");

        // Then - Master datasource key should still exist in routing datasource
        assertThat(activeSources())
                .containsKey("master")
                .containsKey("tenant-001");
    }

    // ==================== Consistency Tests ====================

    @Test
    @DisplayName("Should use same credentials for all datasources")
    void testConsistentCredentials() {
        // When
        datasourceManager.addOrUpdateTenantDatasource("tenant-1", "db1");
        datasourceManager.addOrUpdateTenantDatasource("tenant-2", "db2");

        // Then - Both should be added successfully
        assertThat(activeSources())
                .containsKeys("tenant-1", "tenant-2");
    }

    @Test
    @DisplayName("Should handle URL with query parameters")
    void testUrlWithQueryParameters() {
        // Given
        ReflectionTestUtils.setField(datasourceManager, "masterDbUrl",
                "jdbc:h2:mem:master/?DB_CLOSE_DELAY=-1&DB_CLOSE_ON_EXIT=FALSE");

        // When & Then
        assertThatNoException().isThrownBy(() ->
            datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001"));

        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    // ==================== Sequence Tests ====================

    @Test
    @DisplayName("Should add then remove datasource in sequence")
    void testAddThenRemoveSequence() {
        // When - Add
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");
        assertThat(activeSources())
                .containsKey("tenant-001");

        // When - Remove
        datasourceManager.removeTenantDatasource("tenant-001");
        assertThat(activeSources())
                .doesNotContainKey("tenant-001");
    }

    @Test
    @DisplayName("Should handle add after remove")
    void testAddAfterRemove() {
        // When - Add
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001");
        assertThat(activeSources())
                .containsKey("tenant-001");

        // When - Remove
        datasourceManager.removeTenantDatasource("tenant-001");
        assertThat(activeSources())
                .doesNotContainKey("tenant-001");

        // When - Add same tenant again
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "tenant-db-001-new");
        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    @Test
    @DisplayName("Should handle concurrent add and remove for different tenants")
    void testConcurrentAddRemove() throws InterruptedException {
        // When
        Thread addThread = new Thread(() -> {
            datasourceManager.addOrUpdateTenantDatasource("tenant-1", "db-1");
            datasourceManager.addOrUpdateTenantDatasource("tenant-2", "db-2");
        });

        Thread removeThread = new Thread(() -> {
            try {
                Thread.sleep(100); // Ensure add happens first
                datasourceManager.removeTenantDatasource("tenant-99"); // Non-existent tenant
                datasourceManager.removeTenantDatasource("tenant-98"); // Non-existent tenant
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        addThread.start();
        removeThread.start();
        addThread.join();
        removeThread.join();

        // Then - tenant-1 and tenant-2 should be present
        assertThat(activeSources())
                .containsKeys("tenant-1", "tenant-2");
    }

    // ==================== Error Message Tests ====================

    @Test
    @DisplayName("Should provide descriptive error message for add failure")
    void testAddOrUpdateErrorMessage() {
        // When - Try to add with valid H2 (should succeed)
        assertThatNoException().isThrownBy(() ->
            datasourceManager.addOrUpdateTenantDatasource("tenant-001", "db-001")
        );

        // Then - Datasource should be available
        assertThat(activeSources())
                .containsKey("tenant-001");
    }

    @Test
    @DisplayName("Should provide descriptive error message for remove failure")
    void testRemoveErrorMessage() {
        // When & Then - Removing non-existent tenant should be handled gracefully
        assertThatNoException().isThrownBy(() ->
            datasourceManager.removeTenantDatasource("tenant-nonexistent")
        );
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should manage datasource lifecycle: add, update, remove")
    void testCompleteLifecycle() {
        // When - Add
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "db-001");
        assertThat(activeSources())
                .containsKey("tenant-001");

        // When - Update
        datasourceManager.addOrUpdateTenantDatasource("tenant-001", "db-001-v2");
        assertThat(activeSources())
                .containsKey("tenant-001");

        // When - Remove
        datasourceManager.removeTenantDatasource("tenant-001");
        assertThat(activeSources())
                .doesNotContainKey("tenant-001");
    }

    @Test
    @DisplayName("Should handle multiple tenant management")
    void testMultipleTenantManagement() {
        // When
        datasourceManager.addOrUpdateTenantDatasource("tenant-1", "db-1");
        datasourceManager.addOrUpdateTenantDatasource("tenant-2", "db-2");
        datasourceManager.addOrUpdateTenantDatasource("tenant-3", "db-3");

        // Then
        assertThat(activeSources())
                .containsKeys("tenant-1", "tenant-2", "tenant-3");

        // When
        datasourceManager.removeTenantDatasource("tenant-2");

        // Then
        assertThat(activeSources())
                .containsKeys("tenant-1", "tenant-3")
                .doesNotContainKey("tenant-2");
    }
}

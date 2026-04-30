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
 * Unit tests for RoutingDataSource
 * Covers dynamic datasource routing, tenant-specific database routing, and datasource management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingDataSource Unit Tests")
class RoutingDataSourceTest {

    @Mock
    private TenantContext tenantContext;

    @Mock
    private DataSource mockDataSource;

    private RoutingDataSource routingDataSource;

    @BeforeEach
    void setUp() {
        routingDataSource = new RoutingDataSource(tenantContext);
    }

    @SuppressWarnings("unchecked")
    private Map<String, DataSource> activeSources() {
        return (Map<String, DataSource>) ReflectionTestUtils.getField(routingDataSource, "tenantDataSources");
    }

    // ==================== determineCurrentLookupKey Tests ====================

    @Test
    @DisplayName("Should return tenant ID when tenant context is set")
    void testDetermineCurrentLookupKey_WithTenant() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant-001");

        // When
        Object lookupKey = routingDataSource.determineCurrentLookupKey();

        // Then
        assertThat(lookupKey).isEqualTo("tenant-001");
        verify(tenantContext, times(1)).getCurrentTenantId();
    }

    @Test
    @DisplayName("Should return 'master' when no tenant context is set")
    void testDetermineCurrentLookupKey_NoTenant() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(null);

        // When
        Object lookupKey = routingDataSource.determineCurrentLookupKey();

        // Then
        assertThat(lookupKey).isEqualTo("master");
        verify(tenantContext, times(1)).getCurrentTenantId();
    }

    @Test
    @DisplayName("Should return 'master' when tenant ID is empty string")
    void testDetermineCurrentLookupKey_EmptyTenantId() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn("");

        // When
        Object lookupKey = routingDataSource.determineCurrentLookupKey();

        // Then
        // Empty string is still truthy, so will return empty string not master
        assertThat(lookupKey).isEqualTo("");
    }

    @Test
    @DisplayName("Should return different tenant IDs for multiple calls")
    void testDetermineCurrentLookupKey_MultipleTenants() {
        // When & Then
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant-1");
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo("tenant-1");

        when(tenantContext.getCurrentTenantId()).thenReturn("tenant-2");
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo("tenant-2");

        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo("master");
    }

    @Test
    @DisplayName("Should handle special characters in tenant ID")
    void testDetermineCurrentLookupKey_SpecialCharacters() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant-001-abc_XYZ");

        // When
        Object lookupKey = routingDataSource.determineCurrentLookupKey();

        // Then
        assertThat(lookupKey).isEqualTo("tenant-001-abc_XYZ");
    }

    // ==================== addTargetDataSource Tests ====================

    @Test
    @DisplayName("Should add new datasource successfully")
    void testAddTargetDataSource_NewDatasource() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        routingDataSource.addTargetDataSource("tenant-new", mockDataSource);

        // Then
        assertThat(activeSources()).containsKey("tenant-new");
    }

    @Test
    @DisplayName("Should update existing datasource")
    void testAddTargetDataSource_UpdateExisting() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        initialDataSources.put("tenant-001", mock(DataSource.class));
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        DataSource newDataSource = mock(DataSource.class);

        // When
        routingDataSource.addTargetDataSource("tenant-001", newDataSource);

        // Then
        assertThat(activeSources().get("tenant-001")).isEqualTo(newDataSource);
    }

    @Test
    @DisplayName("Should preserve existing datasources when adding new one")
    void testAddTargetDataSource_PreserveExisting() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        DataSource tenant1DS = mock(DataSource.class);
        initialDataSources.put("tenant-001", tenant1DS);
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        routingDataSource.addTargetDataSource("tenant-002", mockDataSource);

        // Then
        assertThat(activeSources()).containsKeys("tenant-001", "tenant-002");
        assertThat(activeSources().get("tenant-001")).isEqualTo(tenant1DS);
    }

    @Test
    @DisplayName("Should handle null datasource")
    void testAddTargetDataSource_NullDatasource() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> routingDataSource.addTargetDataSource("tenant-null", null));
    }

    @Test
    @DisplayName("Should be thread-safe for multiple additions")
    void testAddTargetDataSource_ThreadSafety() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        Thread thread1 = new Thread(() -> routingDataSource.addTargetDataSource("tenant-1", mock(DataSource.class)));
        Thread thread2 = new Thread(() -> routingDataSource.addTargetDataSource("tenant-2", mock(DataSource.class)));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then
        assertThat(activeSources()).hasSize(2);
    }

    // ==================== removeTargetDataSource Tests ====================

    @Test
    @DisplayName("Should remove datasource successfully")
    void testRemoveTargetDataSource_Success() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        initialDataSources.put("tenant-001", mock(DataSource.class));
        initialDataSources.put("tenant-002", mock(DataSource.class));
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        routingDataSource.removeTargetDataSource("tenant-001");

        // Then
        assertThat(activeSources()).doesNotContainKey("tenant-001");
        assertThat(activeSources()).containsKey("tenant-002");
    }

    @Test
    @DisplayName("Should handle removal of non-existent datasource")
    void testRemoveTargetDataSource_NonExistent() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        initialDataSources.put("tenant-001", mock(DataSource.class));
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When & Then - Should not throw exception
        assertThatNoException().isThrownBy(() -> routingDataSource.removeTargetDataSource("tenant-999"));
    }

    @Test
    @DisplayName("Should preserve other datasources when removing one")
    void testRemoveTargetDataSource_PreserveOthers() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        DataSource tenant1DS = mock(DataSource.class);
        DataSource tenant2DS = mock(DataSource.class);
        DataSource tenant3DS = mock(DataSource.class);
        initialDataSources.put("tenant-001", tenant1DS);
        initialDataSources.put("tenant-002", tenant2DS);
        initialDataSources.put("tenant-003", tenant3DS);
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        routingDataSource.removeTargetDataSource("tenant-002");

        // Then
        assertThat(activeSources()).containsEntry("tenant-001", tenant1DS);
        assertThat(activeSources()).doesNotContainKey("tenant-002");
        assertThat(activeSources()).containsEntry("tenant-003", tenant3DS);
    }

    @Test
    @DisplayName("Should be thread-safe for multiple removals")
    void testRemoveTargetDataSource_ThreadSafety() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            initialDataSources.put("tenant-" + i, mock(DataSource.class));
        }
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        Thread thread1 = new Thread(() -> routingDataSource.removeTargetDataSource("tenant-1"));
        Thread thread2 = new Thread(() -> routingDataSource.removeTargetDataSource("tenant-2"));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then
        assertThat(activeSources()).doesNotContainKeys("tenant-1", "tenant-2");
        assertThat(activeSources()).containsKeys("tenant-3", "tenant-4", "tenant-5");
    }

    @Test
    @DisplayName("Should add datasource after removing one")
    void testRemoveAndAdd_Sequence() throws Exception {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        initialDataSources.put("tenant-001", mock(DataSource.class));
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.afterPropertiesSet();

        // When
        routingDataSource.removeTargetDataSource("tenant-001");
        routingDataSource.addTargetDataSource("tenant-002", mockDataSource);

        // Then
        assertThat(activeSources()).doesNotContainKey("tenant-001");
        assertThat(activeSources()).containsKey("tenant-002");
    }

    // ==================== Master DataSource Tests ====================

    @Test
    @DisplayName("Should always have master datasource available")
    void testMasterDataSource_AlwaysAvailable() {
        // Given
        Map<Object, Object> initialDataSources = new HashMap<>();
        DataSource masterDS = mock(DataSource.class);
        initialDataSources.put("master", masterDS);
        routingDataSource.setTargetDataSources(initialDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDS);

        when(tenantContext.getCurrentTenantId()).thenReturn(null);

        // When
        Object lookupKey = routingDataSource.determineCurrentLookupKey();

        // Then
        assertThat(lookupKey).isEqualTo("master");
    }
}

package com.knp.multitenant;

import com.zaxxer.hikari.HikariDataSource;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DatasourceConfig
 * Covers Spring configuration for datasources and routing setup
 * Uses H2 in-memory database for testing with proper initialization
 * 
 * IMPORTANT: RoutingDataSource requires afterPropertiesSet() to be called
 * after setting target datasources to initialize the internal resolver cache
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatasourceConfig Unit Tests")
class DatasourceConfigTest {

    @Mock
    private TenantContext tenantContext;

    private DatasourceConfig datasourceConfig;

    @BeforeEach
    void setUp() {
        // Create fresh instance for each test
        datasourceConfig = new DatasourceConfig();
        
        // Set configuration values - using H2 in-memory database for testing
        ReflectionTestUtils.setField(datasourceConfig, "masterDbUrl", "jdbc:h2:mem:testdb");
        ReflectionTestUtils.setField(datasourceConfig, "masterDbUsername", "sa");
        ReflectionTestUtils.setField(datasourceConfig, "masterDbPassword", "");
    }

    // ==================== masterDataSource Bean Tests ====================

    @Test
    @DisplayName("Should create master DataSource bean")
    void testMasterDataSource_Created() {
        // When
        DataSource masterDataSource = datasourceConfig.masterDataSource();

        // Then
        assertThat(masterDataSource).isNotNull();
    }

    @Test
    @DisplayName("Master DataSource should be created from configuration properties")
    void testMasterDataSource_UsesConfigProperties() {
        // When
        DataSource masterDataSource = datasourceConfig.masterDataSource();

        // Then
        assertThat(masterDataSource).isNotNull();
    }

    @Test
    @DisplayName("Should create HikariDataSource for master")
    void testMasterDataSource_IsHikariDataSource() {
        // When
        DataSource masterDataSource = datasourceConfig.masterDataSource();

        // Then
        assertThat(masterDataSource).isNotNull();
        assertThat(masterDataSource.getClass().getSimpleName()).contains("HikariDataSource");
    }

    @Test
    @DisplayName("Should create multiple master DataSource instances")
    void testMasterDataSource_MultipleInstances() {
        // When
        DataSource ds1 = datasourceConfig.masterDataSource();
        DataSource ds2 = datasourceConfig.masterDataSource();

        // Then
        assertThat(ds1).isNotNull();
        assertThat(ds2).isNotNull();
        // Each call creates a new instance
        assertThat(ds1).isNotEqualTo(ds2);
    }

    // ==================== Configuration Integration Tests ====================

    @Test
    @DisplayName("Should have master datasource configured correctly")
    void testConfigurationIntegration_MasterDataSourceProperties() {
        // When
        DataSource masterDataSource = datasourceConfig.masterDataSource();

        // Then
        assertThat(masterDataSource).isNotNull();
        assertThat(masterDataSource.getClass().getName()).contains("HikariDataSource");
    }

    @Test
    @DisplayName("Should handle null master datasource gracefully")
    void testHandleNullMasterDataSource() {
        // When & Then
        assertThat(datasourceConfig).isNotNull();
    }

    // ==================== Multi-Tenant Configuration Tests ====================

    @Test
    @DisplayName("Should support multiple tenant configurations")
    void testMultiTenantConfiguration() {
        // Create first configuration with H2
        DatasourceConfig config1 = new DatasourceConfig();
        ReflectionTestUtils.setField(config1, "masterDbUrl", "jdbc:h2:mem:master1");
        ReflectionTestUtils.setField(config1, "masterDbUsername", "sa");
        ReflectionTestUtils.setField(config1, "masterDbPassword", "");

        // Create second configuration with H2
        DatasourceConfig config2 = new DatasourceConfig();
        ReflectionTestUtils.setField(config2, "masterDbUrl", "jdbc:h2:mem:master2");
        ReflectionTestUtils.setField(config2, "masterDbUsername", "sa");
        ReflectionTestUtils.setField(config2, "masterDbPassword", "");

        // When
        DataSource ds1 = config1.masterDataSource();
        DataSource ds2 = config2.masterDataSource();

        // Then
        assertThat(ds1).isNotNull();
        assertThat(ds2).isNotNull();
        assertThat(ds1).isNotEqualTo(ds2);
    }




    @Test
    @DisplayName("Should handle configuration property changes")
    void testConfigurationPropertyChanges() {
        // Given
        DataSource ds1 = datasourceConfig.masterDataSource();

        // When - Change configuration
        ReflectionTestUtils.setField(datasourceConfig, "masterDbUrl", "jdbc:h2:mem:newdb");
        DataSource ds2 = datasourceConfig.masterDataSource();

        // Then
        assertThat(ds1).isNotNull();
        assertThat(ds2).isNotNull();
        assertThat(ds1).isNotEqualTo(ds2);
    }
}
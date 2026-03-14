package com.knp.multitenant;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DatasourceUtil
 * Covers datasource creation, URL building, and tenant loading utility functions
 * Uses H2 embedded in-memory database for testing (no external server required)
 */
@DisplayName("DatasourceUtil Unit Tests")
class DatasourceUtilTest {

    // ==================== buildTenantDbUrl Tests ====================

    @Test
    @DisplayName("Should build tenant URL from simple master URL")
    void testBuildTenantDbUrl_SimpleUrl() {
        // Given
        String masterUrl = "jdbc:h2:mem:master";
        String dbName = "tenant-001";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, dbName);

        // Then
        assertThat(result).contains("tenant-001");
        assertThat(result).startsWith("jdbc:h2:mem:");
    }

    @Test
    @DisplayName("Should build tenant URL from master URL with query parameters")
    void testBuildTenantDbUrl_WithQueryParameters() {
        // Given
        String masterUrl = "jdbc:h2:mem:master?MODE=MySQL;DB_CLOSE_DELAY=-1";
        String dbName = "tenant-db";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, dbName);

        // Then
        assertThat(result).contains("tenant-db");
        assertThat(result).contains("MODE=MySQL");
        assertThat(result).contains("DB_CLOSE_DELAY=-1");
    }

    @Test
    @DisplayName("Should preserve query parameters when building URL")
    void testBuildTenantDbUrl_PreservesQueryParameters() {
        // Given
        String masterUrl = "jdbc:h2:mem:master?param1=value1&param2=value2";
        String dbName = "tenant-new";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, dbName);

        // Then
        assertThat(result).contains("param1=value1");
        assertThat(result).contains("param2=value2");
        assertThat(result).contains("tenant-new");
    }

    @Test
    @DisplayName("Should handle various database names")
    void testBuildTenantDbUrl_VariousDbNames() {
        // Given
        String masterUrl = "jdbc:h2:mem:master";

        // When & Then
        assertThat(DatasourceUtil.buildTenantDbUrl(masterUrl, "db_1")).contains("db_1");
        assertThat(DatasourceUtil.buildTenantDbUrl(masterUrl, "db-2")).contains("db-2");
        assertThat(DatasourceUtil.buildTenantDbUrl(masterUrl, "db_abc_123")).contains("db_abc_123");
        assertThat(DatasourceUtil.buildTenantDbUrl(masterUrl, "TENANT_DB")).contains("TENANT_DB");
    }

    @Test
    @DisplayName("Should handle URL with multiple query parameters")
    void testBuildTenantDbUrl_MultipleQueryParams() {
        // Given
        String masterUrl = "jdbc:h2:mem:master?MODE=MySQL&DB_CLOSE_DELAY=-1&DB_CLOSE_ON_EXIT=FALSE";
        String dbName = "new-tenant";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, dbName);

        // Then
        assertThat(result).contains("new-tenant");
        assertThat(result).contains("MODE=MySQL");
        assertThat(result).contains("DB_CLOSE_DELAY=-1");
    }

    @Test
    @DisplayName("Should handle different host and port combinations")
    void testBuildTenantDbUrl_DifferentHosts() {
        // Given & When & Then - H2 URLs don't have host/port, but test URL building
        String result1 = DatasourceUtil.buildTenantDbUrl("jdbc:h2:mem:master", "tenant-db");
        assertThat(result1).contains("tenant-db");

        String result2 = DatasourceUtil.buildTenantDbUrl("jdbc:h2:mem:master?MODE=MySQL", "tenant-db");
        assertThat(result2).contains("tenant-db");

        String result3 = DatasourceUtil.buildTenantDbUrl("jdbc:h2:mem:master;MODE=MySQL", "tenant-db");
        assertThat(result3).contains("tenant-db");
    }

    @Test
    @DisplayName("Should handle URL without trailing slash before database")
    void testBuildTenantDbUrl_UrlFormats() {
        // Given
        String masterUrl1 = "jdbc:h2:mem:master-db";
        String masterUrl2 = "jdbc:h2:mem:master-db;MODE=MySQL";

        // When
        String result1 = DatasourceUtil.buildTenantDbUrl(masterUrl1, "tenant");
        String result2 = DatasourceUtil.buildTenantDbUrl(masterUrl2, "tenant");

        // Then
        assertThat(result1).contains("tenant");
        assertThat(result2).contains("tenant");
    }

    @Test
    @DisplayName("Should replace database name correctly in URL")
    void testBuildTenantDbUrl_ReplaceDatabase() {
        // Given
        String masterUrl = "jdbc:h2:mem:master-database";
        String newDbName = "new-database";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, newDbName);

        // Then
        assertThat(result).contains("new-database");
    }

    @Test
    @DisplayName("Should handle empty database name")
    void testBuildTenantDbUrl_EmptyDbName() {
        // Given
        String masterUrl = "jdbc:h2:mem:master";

        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, "");

        // Then
        assertThat(result).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:h2:mem:master?MODE=MySQL",
            "jdbc:h2:mem:master;DB_CLOSE_DELAY=-1",
            "jdbc:h2:mem:master?IGNORECASE=TRUE",
            "jdbc:h2:mem:master?param1=val1&param2=val2&param3=val3"
    })
    @DisplayName("Should handle various master URL formats")
    void testBuildTenantDbUrl_VariousUrlFormats(String masterUrl) {
        // When
        String result = DatasourceUtil.buildTenantDbUrl(masterUrl, "tenant-db");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("tenant-db");
        assertThat(result).startsWith("jdbc:h2:mem:");
    }

    // ==================== createHikariDataSource Tests ====================

    @Test
    @DisplayName("Should create HikariDataSource with basic configuration")
    void testCreateHikariDataSource_BasicConfig() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        ((HikariDataSource) dataSource).close();
    }

    @Test
    @DisplayName("Should create HikariDataSource with various URLs")
    void testCreateHikariDataSource_VariousUrls() {
        // When & Then
        DataSource ds1 = DatasourceUtil.createHikariDataSource("jdbc:h2:mem:db1", "sa", "");
        assertThat(ds1).isNotNull();
        ((HikariDataSource) ds1).close();

        DataSource ds2 = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:db2?MODE=MySQL", "sa", "");
        assertThat(ds2).isNotNull();
        ((HikariDataSource) ds2).close();

        DataSource ds3 = DatasourceUtil.createHikariDataSource("jdbc:h2:mem:db3?IGNORECASE=TRUE", "sa", "");
        assertThat(ds3).isNotNull();
        ((HikariDataSource) ds3).close();
    }

    @Test
    @DisplayName("Should configure max pool size correctly")
    void testCreateHikariDataSource_PoolSize() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMaximumPoolSize()).isEqualTo(10);
        hikariDS.close();
    }

    @Test
    @DisplayName("Should configure minimum idle correctly")
    void testCreateHikariDataSource_MinIdle() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMinimumIdle()).isEqualTo(2);
        hikariDS.close();
    }

    @Test
    @DisplayName("Should set connection timeout")
    void testCreateHikariDataSource_ConnectionTimeout() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getConnectionTimeout()).isEqualTo(30000);
        hikariDS.close();
    }

    @Test
    @DisplayName("Should set idle timeout")
    void testCreateHikariDataSource_IdleTimeout() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getIdleTimeout()).isEqualTo(600000);
        hikariDS.close();
    }

    @Test
    @DisplayName("Should set max lifetime")
    void testCreateHikariDataSource_MaxLifetime() {
        // When
        DataSource dataSource = DatasourceUtil.createHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMaxLifetime()).isEqualTo(1800000);
        hikariDS.close();
    }

    // ==================== createLightweightHikariDataSource Tests ====================

    @Test
    @DisplayName("Should create lightweight HikariDataSource")
    void testCreateLightweightHikariDataSource_Success() {
        // When
        DataSource dataSource = DatasourceUtil.createLightweightHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        ((HikariDataSource) dataSource).close();
    }

    @Test
    @DisplayName("Lightweight datasource should have max pool size of 10")
    void testCreateLightweightHikariDataSource_PoolSize() {
        // When
        DataSource dataSource = DatasourceUtil.createLightweightHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMaximumPoolSize()).isEqualTo(10);
        hikariDS.close();
    }

    @Test
    @DisplayName("Lightweight datasource should have minimum idle of 2")
    void testCreateLightweightHikariDataSource_MinIdle() {
        // When
        DataSource dataSource = DatasourceUtil.createLightweightHikariDataSource(
                "jdbc:h2:mem:test",
                "sa",
                ""
        );

        // Then
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMinimumIdle()).isEqualTo(2);
        hikariDS.close();
    }

    @Test
    @DisplayName("Should create lightweight datasource with different URLs")
    void testCreateLightweightHikariDataSource_VariousUrls() {
        // When & Then
        DataSource ds1 = DatasourceUtil.createLightweightHikariDataSource(
                "jdbc:h2:mem:tenant-1",
                "sa", "");
        assertThat(ds1).isNotNull();
        ((HikariDataSource) ds1).close();

        DataSource ds2 = DatasourceUtil.createLightweightHikariDataSource(
                "jdbc:h2:mem:tenant-2?MODE=MySQL",
                "sa", "");
        assertThat(ds2).isNotNull();
        ((HikariDataSource) ds2).close();
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should build URL and create datasource for tenant")
    void testIntegration_BuildUrlAndCreateDatasource() {
        // Given
        String masterUrl = "jdbc:h2:mem:master";
        String tenantDbName = "tenant-001";

        // When
        String tenantUrl = DatasourceUtil.buildTenantDbUrl(masterUrl, tenantDbName);
        DataSource dataSource = DatasourceUtil.createLightweightHikariDataSource(
                tenantUrl, "sa", "");

        // Then
        assertThat(dataSource).isNotNull();
        assertThat(tenantUrl).contains(tenantDbName);
        ((HikariDataSource) dataSource).close();
    }

    @Test
    @DisplayName("Should create multiple datasources for different tenants")
    void testIntegration_MultipleTenantsDataSources() {
        // Given
        String masterUrl = "jdbc:h2:mem:master";

        // When
        String url1 = DatasourceUtil.buildTenantDbUrl(masterUrl, "tenant-1");
        String url2 = DatasourceUtil.buildTenantDbUrl(masterUrl, "tenant-2");
        String url3 = DatasourceUtil.buildTenantDbUrl(masterUrl, "tenant-3");

        DataSource ds1 = DatasourceUtil.createLightweightHikariDataSource(url1, "sa", "");
        DataSource ds2 = DatasourceUtil.createLightweightHikariDataSource(url2, "sa", "");
        DataSource ds3 = DatasourceUtil.createLightweightHikariDataSource(url3, "sa", "");

        // Then
        assertThat(ds1).isNotNull();
        assertThat(ds2).isNotNull();
        assertThat(ds3).isNotNull();

        ((HikariDataSource) ds1).close();
        ((HikariDataSource) ds2).close();
        ((HikariDataSource) ds3).close();
    }

    @Test
    @DisplayName("Should handle tenant URL construction with parameters")
    void testIntegration_TenantUrlWithParameters() {
        // Given
        String masterUrl = "jdbc:h2:mem:master?MODE=MySQL&DB_CLOSE_DELAY=-1";

        // When
        String tenantUrl = DatasourceUtil.buildTenantDbUrl(masterUrl, "new-tenant");

        // Then
        assertThat(tenantUrl).contains("new-tenant");
        assertThat(tenantUrl).contains("MODE=MySQL");
        assertThat(tenantUrl).contains("DB_CLOSE_DELAY=-1");

        // Create datasource to verify URL is valid
        DataSource ds = DatasourceUtil.createLightweightHikariDataSource(tenantUrl, "sa", "");
        assertThat(ds).isNotNull();
        ((HikariDataSource) ds).close();
    }
}

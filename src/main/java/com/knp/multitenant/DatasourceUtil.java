package com.knp.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * DatasourceUtil - Utility class for datasource operations
 * Centralizes shared datasource creation and configuration logic
 */
@Slf4j
public class DatasourceUtil {

    // Default driver for MySQL (production)
    private static final String DEFAULT_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    // H2 driver for testing
    private static final String H2_DRIVER_CLASS = "org.h2.Driver";
    
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    private static final long CONNECTION_TIMEOUT = 30000;
    private static final long IDLE_TIMEOUT = 600000;
    private static final long MAX_LIFETIME = 1800000;

    private DatasourceUtil() {
        // Utility class - no instantiation
    }

    /**
     * Determine JDBC driver class based on the JDBC URL
     * Automatically detects H2 or MySQL driver needed
     */
    private static String getDriverClass(String jdbcUrl) {
        if (jdbcUrl != null) {
            if (jdbcUrl.contains("h2:")) {
                log.debug("Detected H2 database URL, using H2 driver");
                return H2_DRIVER_CLASS;
            } else if (jdbcUrl.contains("mysql:")) {
                log.debug("Detected MySQL database URL, using MySQL driver");
                return DEFAULT_DRIVER_CLASS;
            }
        }
        // Default to MySQL if unable to determine
        log.debug("Unable to determine driver from URL, defaulting to MySQL");
        return DEFAULT_DRIVER_CLASS;
    }

    /**
     * Create a HikariDataSource with standard configuration
     *
     * @param jdbcUrl  the JDBC URL
     * @param username database username
     * @param password database password
     * @return configured HikariDataSource
     */
    public static DataSource createHikariDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(getDriverClass(jdbcUrl));
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setIdleTimeout(IDLE_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);
        return new HikariDataSource(config);
    }

    /**
     * Create a lightweight HikariDataSource (used for dynamic tenant datasources)
     *
     * @param jdbcUrl  the JDBC URL
     * @param username database username
     * @param password database password
     * @return configured HikariDataSource
     */
    public static DataSource createLightweightHikariDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(getDriverClass(jdbcUrl));
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        return new HikariDataSource(config);
    }

    /**
     * Build tenant database URL from master URL and tenant database name
     *
     * @param masterDbUrl the master database URL
     * @param dbName      the tenant's database name
     * @return constructed tenant database URL
     */
    public static String buildTenantDbUrl(String masterDbUrl, String dbName) {
        String baseUrl = masterDbUrl;

        if (baseUrl.contains("?")) {
            int questionMark = baseUrl.indexOf("?");
            String params = baseUrl.substring(questionMark);
            baseUrl = baseUrl.substring(0, questionMark);

            if (baseUrl.contains("/")) {
                int lastSlash = baseUrl.lastIndexOf("/");
                baseUrl = baseUrl.substring(0, lastSlash + 1);
            }

            return baseUrl + dbName + params;
        } else {
            if (baseUrl.contains("/")) {
                int lastSlash = baseUrl.lastIndexOf("/");
                baseUrl = baseUrl.substring(0, lastSlash + 1);
            }
            return baseUrl + dbName;
        }
    }

    /**
     * Load all active tenant datasources from master database
     *
     * @param masterDataSource the master datasource to query
     * @param masterDbUrl      the master database URL
     * @param username         database username
     * @param password         database password
     * @return map of tenant datasources keyed by tenant ID
     */
    public static Map<String, Object> loadTenantsFromMasterDb(
            DataSource masterDataSource, String masterDbUrl, String username, String password) {
        Map<String, Object> tenants = new HashMap<>();

        try (Connection conn = masterDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tenant_id, db_name FROM tenants WHERE active = true")) {

            while (rs.next()) {
                String tenantId = rs.getString("tenant_id");
                String dbName = rs.getString("db_name");

                String tenantDbUrl = buildTenantDbUrl(masterDbUrl, dbName);
                log.debug("Loading tenant configuration for tenantId: {}, dbName: {}", tenantId, dbName);

                DataSource tenantDataSource = createLightweightHikariDataSource(tenantDbUrl, username, password);
                tenants.put(tenantId, tenantDataSource);
            }
        } catch (SQLException e) {
            log.error("Failed to load tenant configurations from master database", e);
            throw new RuntimeException("Failed to load tenant configurations", e);
        }

        return tenants;
    }
}


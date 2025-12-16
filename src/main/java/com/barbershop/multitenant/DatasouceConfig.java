package com.barbershop.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * PersistenceConfig - Configures data source and JPA/Hibernate
 * <p>
 * Sets up:
 * 1. Master DataSource - for tenant metadata
 * 2. Routing DataSource - switches between tenant databases
 * 3. JPA/Hibernate configuration
 */
@Configuration
public class DatasouceConfig {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String masterDbUsername;

    @Value("${spring.datasource.password}")
    private String masterDbPassword;

    /**
     * Master DataSource - stores tenant metadata
     * Used to query which databases tenants have
     *
     * @return master DataSource
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterDbUrl);
        config.setUsername(masterDbUsername);
        config.setPassword(masterDbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    /**
     * Routing DataSource - routes queries to tenant-specific databases
     * Primary DataSource used by application
     *
     * @param tenantContext    context for current tenant
     * @param masterDataSource master database connection
     * @return routing DataSource
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource routingDataSource(TenantContext tenantContext,
                                        @Qualifier("masterDataSource") DataSource masterDataSource) {
        RoutingDataSource routingDataSource =
                new RoutingDataSource(tenantContext);

        // Set master DataSource as default and also for "master" lookup key
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);

        // Load all tenant databases at runtime
        Map<String, Object> tenants = loadTenantsFromMasterDb(masterDataSource);
        targetDataSources.putAll(tenants);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        return routingDataSource;
    }

    /**
     * Load tenant configurations from master database
     */
    private Map<String, Object> loadTenantsFromMasterDb(DataSource masterDataSource) {
        Map<String, Object> tenants = new HashMap<>();

        try (Connection conn = masterDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tenant_id, db_url, db_username, db_password FROM tenants")) {

            while (rs.next()) {
                String tenantId = rs.getString("tenant_id");
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(rs.getString("db_url"));
                config.setUsername(rs.getString("db_username"));
                config.setPassword(rs.getString("db_password"));
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);

                tenants.put(tenantId, new HikariDataSource(config));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tenant configurations", e);
        }

        return tenants;
    }
}


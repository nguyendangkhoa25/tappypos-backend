package com.knp.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DatasourceConfig - Configures data source and JPA/Hibernate
 * <p>
 * Sets up:
 * 1. Master DataSource - for tenant metadata
 * 2. Routing DataSource - switches between tenant databases
 * 3. JPA/Hibernate configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DatasourceConfig {

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
        return DatasourceUtil.createHikariDataSource(masterDbUrl, masterDbUsername, masterDbPassword);
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
        RoutingDataSource routingDataSource = new RoutingDataSource(tenantContext);

        // Set master DataSource as default and also for "master" lookup key
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);

        // Load all tenant databases at runtime
        Map<String, Object> tenants = DatasourceUtil.loadTenantsFromMasterDb(
                masterDataSource, masterDbUrl, masterDbUsername, masterDbPassword);
        targetDataSources.putAll(tenants);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        return routingDataSource;
    }
}


package com.knp.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Datasource Configuration - Overrides the production DatasouceConfig
 * Uses H2 in-memory database for testing
 */
@TestConfiguration
@Slf4j
public class TestDatasouceConfig {

    /**
     * Test master DataSource using H2 in-memory database
     */
    @Bean(name = "masterDataSource")
    @Primary
    public DataSource testMasterDataSource() {
        log.info("Creating H2 test datasource");
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=MYSQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * Test routing DataSource - routes queries to H2 test DB
     */
    @Bean(name = "dataSource")
    public DataSource testRoutingDataSource(TenantContext tenantContext) {
        log.info("Creating test routing datasource");
        RoutingDataSource routingDataSource = new RoutingDataSource(tenantContext);
        
        // Set master DataSource as default
        DataSource h2DataSource = testMasterDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", h2DataSource);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(h2DataSource);
        return routingDataSource;
    }
}


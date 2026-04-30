package com.knp.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DatasourceManager - Manages dynamic tenant datasource reloading
 * Allows adding, removing, and reloading tenant datasources without app restart
 */
@Service
@Slf4j
public class DatasourceManager {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String masterDbUsername;

    @Value("${spring.datasource.password}")
    private String masterDbPassword;

    private final RoutingDataSource routingDataSource;
    private final DataSource masterDataSource;

    public DatasourceManager(
            @Qualifier("dataSource") RoutingDataSource routingDataSource,
            @Qualifier("masterDataSource") DataSource masterDataSource) {
        this.routingDataSource = routingDataSource;
        this.masterDataSource = masterDataSource;
    }

    /**
     * Reload all tenant datasources from master database
     * Called when tenants are created, deleted, or activated/deactivated
     */
    public void reloadAllTenantDatasource() {
        log.info("Reloading all tenant datasources");
        try {
            Map<String, DataSource> allDataSources = new HashMap<>();
            allDataSources.put("master", masterDataSource);

            Map<String, Object> tenants = DatasourceUtil.loadTenantsFromMasterDb(
                    masterDataSource, masterDbUrl, masterDbUsername, masterDbPassword);
            tenants.forEach((k, v) -> allDataSources.put(k, (DataSource) v));

            routingDataSource.reloadTargetDataSources(allDataSources);
            log.info("Successfully reloaded all tenant datasources. Active tenants: {}", tenants.size());
        } catch (Exception e) {
            log.error("Failed to reload tenant datasources", e);
            throw new RuntimeException("Failed to reload tenant datasources", e);
        }
    }

    /**
     * Add or update a single tenant datasource
     * Used when a new tenant is created or an inactive tenant is activated
     */
    public void addOrUpdateTenantDatasource(String tenantId, String dbName) {
        log.info("Adding/updating datasource for tenant: {}, dbName: {}", tenantId, dbName);
        try {
            String tenantDbUrl = DatasourceUtil.buildTenantDbUrl(masterDbUrl, dbName);
            DataSource tenantDataSource = DatasourceUtil.createLightweightHikariDataSource(
                    tenantDbUrl, masterDbUsername, masterDbPassword);

            // Add or replace tenant datasource
            routingDataSource.addTargetDataSource(tenantId, tenantDataSource);
            log.info("Successfully added/updated datasource for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to add/update datasource for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to add/update datasource for tenant: " + tenantId, e);
        }
    }

    /**
     * Remove a tenant datasource
     * Used when a tenant is deleted or deactivated
     */
    public void removeTenantDatasource(String tenantId) {
        log.info("Removing datasource for tenant: {}", tenantId);
        try {
            routingDataSource.removeTargetDataSource(tenantId);
            log.info("Successfully removed datasource for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to remove datasource for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to remove datasource for tenant: " + tenantId, e);
        }
    }
}


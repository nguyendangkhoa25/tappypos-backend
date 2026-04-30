package com.knp.service.tenant;

import com.knp.model.enums.ShopType;
import com.knp.multitenant.DatasourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

/**
 * Creates a tenant database and initialises it with DDL (mandatory) and
 * optional shop-type-specific DML before the datasource pool is registered.
 *
 * Call {@link #setup} BEFORE {@code DatasourceManager.addOrUpdateTenantDatasource}
 * so the database and schema exist when HikariCP opens its first connections.
 */
@Service
@Slf4j
public class TenantDatabaseSetupService {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final DataSource masterDataSource;

    private static final String DDL_SCRIPT = "db/tenant/ddl.sql";

    private static final String GENERAL_DML      = "db/tenant/general.sql";
    private static final String PAWN_DML         = "db/tenant/pawn_shop.sql";
    private static final String CONVENIENCE_DML  = "db/tenant/convenience_store.sql";

    /** Map each shop type to its seed-data script. */
    private static final Map<ShopType, String> DML_SCRIPTS = Map.ofEntries(
        Map.entry(ShopType.CONVENIENCE_STORE, CONVENIENCE_DML),
        Map.entry(ShopType.PAWN_SHOP,         PAWN_DML),
        Map.entry(ShopType.JEWELRY,           GENERAL_DML),
        Map.entry(ShopType.PHARMACY,          GENERAL_DML),
        Map.entry(ShopType.ELECTRONICS,       GENERAL_DML),
        Map.entry(ShopType.FOOD_BEVERAGE,     GENERAL_DML),
        Map.entry(ShopType.FASHION,           GENERAL_DML),
        Map.entry(ShopType.BARBER_SHOP,       GENERAL_DML),
        Map.entry(ShopType.COFFEE_SHOP,       GENERAL_DML),
        Map.entry(ShopType.RESTAURANT,        GENERAL_DML),
        Map.entry(ShopType.OTHER,             GENERAL_DML)
    );

    public TenantDatabaseSetupService(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    /**
     * Creates the MySQL database (idempotent), applies the full DDL, then runs
     * the shop-type DML if a script exists for {@code shopType}.
     *
     * @param dbName   the database name (e.g. {@code retail-platform-shop001})
     * @param shopType the shop type that controls which DML is seeded
     */
    public void setup(String dbName, ShopType shopType) {
        log.info("Setting up tenant database '{}' for shopType={}", dbName, shopType);
        createDatabase(dbName);
        runScript(dbName, DDL_SCRIPT);
        String dmlScript = DML_SCRIPTS.get(shopType);
        if (dmlScript != null) {
            runScript(dbName, dmlScript);
            log.info("Applied DML '{}' for shopType={}", dmlScript, shopType);
        } else {
            log.info("No DML script defined for shopType={}, skipping", shopType);
        }
        log.info("Tenant database '{}' setup complete", dbName);
    }

    private void createDatabase(String dbName) {
        try (Connection conn = masterDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE DATABASE IF NOT EXISTS `" + dbName + "` " +
                "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
            log.info("Database '{}' created (or already exists)", dbName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database: " + dbName, e);
        }
    }

    private void runScript(String dbName, String scriptPath) {
        String tenantDbUrl = DatasourceUtil.buildTenantDbUrl(masterDbUrl, dbName);
        try (Connection conn = DriverManager.getConnection(tenantDbUrl, dbUsername, dbPassword)) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(scriptPath));
            log.debug("Executed '{}' on '{}'", scriptPath, dbName);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to execute '" + scriptPath + "' on database '" + dbName + "': " + e.getMessage(), e);
        }
    }
}

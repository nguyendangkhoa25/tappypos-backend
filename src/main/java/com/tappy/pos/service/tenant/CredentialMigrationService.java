package com.tappy.pos.service.tenant;

import com.tappy.pos.config.EncryptionService;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time migration utility: encrypts any plaintext e-invoice credentials
 * that were stored before the EncryptedStringConverter was applied.
 *
 * Uses raw JDBC (tenant context controls row-level filtering via PostgreSQL RLS)
 * so that the JPA converter is bypassed and we can both read the raw plaintext
 * and write back the encrypted value in one pass.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialMigrationService {

    private final TenantRepository tenantRepository;
    private final TenantContext    tenantContext;
    private final EncryptionService encryptionService;
    private final DataSource        dataSource;

    private static final String[] CREDENTIAL_COLUMNS = {"e_invoice_password", "e_invoice_key"};

    /**
     * Iterates every active tenant and encrypts any plaintext credential values
     * found in shop_info.
     *
     * @return map of tenantId → number of rows updated
     */
    public Map<String, Integer> encryptAllTenants() {
        List<Tenant> tenants = tenantRepository.findAllByActiveTrue();
        Map<String, Integer> results = new LinkedHashMap<>();

        for (Tenant tenant : tenants) {
            try {
                tenantContext.setCurrentTenant(tenant);
                int count = encryptForCurrentTenant();
                results.put(tenant.getTenantId(), count);
                log.info("Credential migration: tenant={} rows_updated={}", tenant.getTenantId(), count);
            } catch (Exception e) {
                log.error("Credential migration failed for tenant {}", tenant.getTenantId(), e);
                results.put(tenant.getTenantId(), -1);
            } finally {
                tenantContext.clear();
            }
        }
        return results;
    }

    // ── per-tenant ─────────────────────────────────────────────────────────────

    private int encryptForCurrentTenant() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, e_invoice_password, e_invoice_key FROM shop_info WHERE deleted = 0");

        int updated = 0;
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            boolean rowUpdated = false;

            for (String col : CREDENTIAL_COLUMNS) {
                String value = (String) row.get(col);
                if (value == null || value.isBlank() || encryptionService.isEncrypted(value)) continue;

                try {
                    String encrypted = encryptionService.encrypt(value);
                    jdbc.update("UPDATE shop_info SET " + col + " = ? WHERE id = ?", encrypted, id);
                    rowUpdated = true;
                    log.debug("Encrypted {} for shop_info.id={}", col, id);
                } catch (Exception e) {
                    log.error("Failed to encrypt {} for shop_info.id={}", col, id, e);
                }
            }
            if (rowUpdated) updated++;
        }
        return updated;
    }
}

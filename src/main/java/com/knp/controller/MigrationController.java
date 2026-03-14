package com.knp.controller;

import com.knp.annotation.MasterDatabaseOnly;
import com.knp.model.dto.ApiResponse;
import com.knp.service.CredentialMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final CredentialMigrationService migrationService;

    /**
     * One-time endpoint: encrypts any plaintext e-invoice credentials across
     * all active tenant databases.
     *
     * Safe to call multiple times — already-encrypted values are skipped.
     * Requires MASTER_TENANT role (enforced by @MasterDatabaseOnly).
     */
    @PostMapping("/encrypt-shop-credentials")
    @MasterDatabaseOnly
    public ResponseEntity<ApiResponse<Map<String, Integer>>> encryptCredentials() {
        log.info("Admin triggered credential encryption migration");
        Map<String, Integer> results = migrationService.encryptAllTenants();
        int totalUpdated = results.values().stream().filter(v -> v >= 0).mapToInt(Integer::intValue).sum();
        String msg = "Migration complete. Tenants processed: " + results.size() + ", rows updated: " + totalUpdated;
        log.info(msg);
        return ResponseEntity.ok(ApiResponse.success(results, msg));
    }
}

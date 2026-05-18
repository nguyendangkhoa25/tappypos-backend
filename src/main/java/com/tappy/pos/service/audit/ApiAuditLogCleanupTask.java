package com.tappy.pos.service.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for audit log maintenance
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "audit.log.cleanup.enabled", havingValue = "true")
public class ApiAuditLogCleanupTask {

    private final ApiAuditLogService auditLogService;

    /**
     * Cleanup old audit logs every day at 2 AM
     * Keeps logs for 90 days by default (configurable via property)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldLogs() {
        try {
            log.info("Starting audit log cleanup task");
            long deletedCount = auditLogService.cleanupOldLogs(90);
            log.info("Audit log cleanup completed. Deleted {} records", deletedCount);
        } catch (Exception e) {
            log.error("Error during audit log cleanup", e);
        }
    }
}


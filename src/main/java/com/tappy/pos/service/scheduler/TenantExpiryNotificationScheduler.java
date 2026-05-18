package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs daily at 03:00 and sends BILLING notifications to all SHOP_OWNER users
 * of every tenant whose subscription expires in exactly 7, 3, or 1 day(s).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantExpiryNotificationScheduler {

    private static final int[] WARN_DAYS = {7, 3, 1};

    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;
    private final TenantContext tenantContext;

    @Scheduled(cron = "0 0 3 * * *")
    public void sendExpiryWarningNotifications() {
        log.info("Tenant expiry check started");

        for (int days : WARN_DAYS) {
            LocalDate targetDate = LocalDate.now().plusDays(days);
            tenantContext.clear();

            List<Tenant> expiring = tenantRepository.findActiveTenantsExpiringOn(targetDate);
            if (expiring.isEmpty()) {
                log.debug("No tenants expiring in {} day(s)", days);
                continue;
            }

            log.info("Sending {}-day expiry warning to {} tenant(s)", days, expiring.size());

            for (Tenant tenant : expiring) {
                try {
                    tenantContext.setCurrentTenant(tenant);
                    notificationService.pushExpiryWarning(tenant, days);
                } catch (Exception e) {
                    log.error("Failed to push {}-day expiry warning for tenant {}",
                            days, tenant.getTenantId(), e);
                } finally {
                    tenantContext.clear();
                }
            }
        }

        log.info("Tenant expiry check completed");
    }
}

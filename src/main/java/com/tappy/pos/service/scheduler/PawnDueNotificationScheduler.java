package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Runs daily at 08:00 and notifies SHOP_OWNER of every active tenant
 * that has the PAWN feature about pawn contracts due today.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PawnDueNotificationScheduler {

    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final SchedulerNotificationHelper helper;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendPawnDueNotifications() {
        log.info("Pawn due notification scheduler started");
        List<Tenant> tenants = tenantRepository.findAllByActiveTrue();

        for (Tenant tenant : tenants) {
            if (!hasFeature(tenant, "PAWN")) continue;
            try {
                tenantContext.setCurrentTenant(tenant);
                helper.sendPawnDueNotification(tenant);
            } catch (Exception e) {
                log.error("Failed to send pawn due notification for tenant {}: {}",
                        tenant.getTenantId(), e.getMessage(), e);
            } finally {
                tenantContext.clear();
            }
        }

        log.info("Pawn due notification scheduler completed");
    }

    private boolean hasFeature(Tenant tenant, String feature) {
        return tenant.getFeatures() != null
                && Arrays.asList(tenant.getFeatures().split(",")).contains(feature);
    }
}

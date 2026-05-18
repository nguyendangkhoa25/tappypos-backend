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
 * Runs daily at 22:00 and sends a revenue digest to SHOP_OWNER of every active tenant
 * that has the ORDER feature.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DailyRevenueSummaryScheduler {

    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final SchedulerNotificationHelper helper;

    @Scheduled(cron = "0 0 22 * * *")
    public void sendDailyRevenueSummaries() {
        log.info("Daily revenue summary scheduler started");
        List<Tenant> tenants = tenantRepository.findAllByActiveTrue();

        for (Tenant tenant : tenants) {
            if (!hasFeature(tenant, "ORDER")) continue;
            try {
                tenantContext.setCurrentTenant(tenant);
                helper.sendDailyRevenueSummary(tenant);
            } catch (Exception e) {
                log.error("Failed to send daily revenue summary for tenant {}: {}",
                        tenant.getTenantId(), e.getMessage(), e);
            } finally {
                tenantContext.clear();
            }
        }

        log.info("Daily revenue summary scheduler completed");
    }

    private boolean hasFeature(Tenant tenant, String feature) {
        return tenant.getFeatures() != null
                && Arrays.asList(tenant.getFeatures().split(",")).contains(feature);
    }
}

package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.installment.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Runs daily at 08:05 and notifies SHOP_OWNER of every active tenant that has the
 * INSTALLMENT feature about overdue trả-góp kỳ. Mirrors {@link PawnDueNotificationScheduler}.
 * See VEHICLE_SHOP_SHOP_TYPE_PLAN §4e.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstallmentOverdueScheduler {

    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final InstallmentService installmentService;

    @Scheduled(cron = "0 5 8 * * *")
    public void sendOverdueNotifications() {
        log.info("Installment overdue notification scheduler started");
        List<Tenant> tenants = tenantRepository.findAllByActiveTrue();
        for (Tenant tenant : tenants) {
            if (!hasFeature(tenant, "INSTALLMENT")) continue;
            try {
                tenantContext.setCurrentTenant(tenant);
                installmentService.notifyOverdue();
            } catch (Exception e) {
                log.error("Failed installment overdue notification for tenant {}: {}",
                        tenant.getTenantId(), e.getMessage(), e);
            } finally {
                tenantContext.clear();
            }
        }
        log.info("Installment overdue notification scheduler completed");
    }

    private boolean hasFeature(Tenant tenant, String feature) {
        return tenant.getFeatures() != null
                && Arrays.asList(tenant.getFeatures().split(",")).contains(feature);
    }
}

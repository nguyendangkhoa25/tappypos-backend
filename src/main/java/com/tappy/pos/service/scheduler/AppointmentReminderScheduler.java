package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Runs at the top of every hour and sends Zalo ZNS appointment reminders to customers
 * whose appointment is scheduled within the next 60 minutes.
 *
 * <p>Only processes active tenants that have the APPOINTMENT feature.
 * The actual reminder logic (DB query + ZNS send + mark sent) is delegated to
 * {@link SchedulerNotificationHelper#sendAppointmentReminders} which runs inside
 * a tenant-scoped transaction so RLS policies apply.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final SchedulerNotificationHelper helper;

    @Scheduled(cron = "0 0 * * * *")   // top of every hour
    public void sendReminders() {
        // Capture the window start at the exact hour (strip minutes/seconds/nanos).
        LocalTime windowStart = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
        log.info("Appointment reminder scheduler started — window: {}–{}", windowStart, windowStart.plusMinutes(60));

        List<Tenant> tenants = tenantRepository.findAllByActiveTrue();
        int processed = 0;

        for (Tenant tenant : tenants) {
            if (!hasFeature(tenant, "APPOINTMENT")) continue;
            try {
                tenantContext.setCurrentTenant(tenant);
                helper.sendAppointmentReminders(tenant, windowStart);
                processed++;
            } catch (Exception e) {
                log.error("Appointment reminder failed for tenant {}: {}",
                        tenant.getTenantId(), e.getMessage(), e);
            } finally {
                tenantContext.clear();
            }
        }

        log.info("Appointment reminder scheduler completed — {} tenant(s) processed", processed);
    }

    private boolean hasFeature(Tenant tenant, String feature) {
        return tenant.getFeatures() != null
                && Arrays.asList(tenant.getFeatures().split(",")).contains(feature);
    }
}

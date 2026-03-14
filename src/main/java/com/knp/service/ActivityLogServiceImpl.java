package com.knp.service;

import com.knp.model.dto.ActivityLogDTO;
import com.knp.model.entity.ActivityLog;
import com.knp.model.entity.Tenant;
import com.knp.model.enums.ActivityAction;
import com.knp.multitenant.TenantContext;
import com.knp.repository.ActivityLogRepository;
import com.knp.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;

    @Override
    @Async
    public void logAsync(String tenantId, String actorUsername, String actorFullName,
                         ActivityAction action, String targetType, String targetId,
                         String description, String ipAddress) {
        if (tenantId == null || actorUsername == null) return;

        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("ActivityLog: tenant not found for id={}", tenantId);
            return;
        }

        tenantContext.setCurrentTenant(tenant);
        try {
            ActivityLog entry = ActivityLog.builder()
                    .actorUsername(actorUsername)
                    .actorFullName(actorFullName)
                    .action(action.name())
                    .targetType(targetType)
                    .targetId(targetId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception e) {
            log.error("ActivityLog: failed to save entry for tenant={} action={}: {}",
                    tenantId, action, e.getMessage());
        } finally {
            tenantContext.clear();
        }
    }

    @Override
    public Page<ActivityLogDTO> getActivityLogs(String actorUsername, String action,
                                                LocalDateTime from, LocalDateTime to,
                                                Pageable pageable) {
        String usernameFilter = (actorUsername != null && actorUsername.isBlank()) ? null : actorUsername;
        String actionFilter = (action != null && action.isBlank()) ? null : action;

        return activityLogRepository
                .findWithFilters(usernameFilter, actionFilter, from, to, pageable)
                .map(ActivityLogDTO::from);
    }
}

package com.tappy.pos.service.audit;

import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.entity.audit.ActivityLog;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.audit.ActivityLogRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
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
        if (tenantId == null || tenantId.isBlank() || actorUsername == null) return;

        boolean isMaster = "master".equalsIgnoreCase(tenantId);
        if (!isMaster) {
            Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
            if (tenant == null) {
                log.warn("ActivityLog: tenant not found for id={}", tenantId);
                return;
            }
            tenantContext.setCurrentTenant(tenant);
        }

        try {
            ActivityLog entry = ActivityLog.builder()
                    .tenantId(isMaster ? null : tenantId)
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
                                                String targetType,
                                                LocalDateTime from, LocalDateTime to,
                                                Pageable pageable) {
        String usernameFilter = (actorUsername != null && actorUsername.isBlank()) ? null : actorUsername;
        String actionFilter = (action != null && action.isBlank()) ? null : action;
        String targetTypeFilter = (targetType != null && targetType.isBlank()) ? null : targetType;

        return activityLogRepository
                .findWithFilters(usernameFilter, actionFilter, targetTypeFilter, from, to, pageable)
                .map(ActivityLogDTO::from);
    }
}

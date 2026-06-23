package com.tappy.pos.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.entity.audit.ActivityLog;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.audit.ActivityLogRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
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

    private static final Object[] NO_ARGS = new Object[0];

    private final ActivityLogRepository activityLogRepository;
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void logAsync(String tenantId, String actorUsername, String actorFullName,
                         ActivityAction action, String targetType, String targetId,
                         String messageKey, String ipAddress, Object... args) {
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
                    .descriptionKey(messageKey)
                    .descriptionArgs(serializeArgs(args))
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
                .map(this::toDto);
    }

    /** Build the DTO, rendering the description in the reader's current locale. */
    private ActivityLogDTO toDto(ActivityLog logEntry) {
        return ActivityLogDTO.builder()
                .id(logEntry.getId())
                .actorUsername(logEntry.getActorUsername())
                .actorFullName(logEntry.getActorFullName())
                .action(logEntry.getAction())
                .targetType(logEntry.getTargetType())
                .targetId(logEntry.getTargetId())
                .description(renderDescription(logEntry))
                .ipAddress(logEntry.getIpAddress())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    /**
     * Prefer the i18n key (rendered in the reader's locale); fall back to the legacy frozen
     * {@code description} for pre-V036 rows or if the key cannot be resolved.
     */
    private String renderDescription(ActivityLog logEntry) {
        String key = logEntry.getDescriptionKey();
        if (key == null || key.isBlank()) {
            return logEntry.getDescription();
        }
        try {
            return messageService.getMessage(key, deserializeArgs(logEntry.getDescriptionArgs()));
        } catch (Exception e) {
            log.warn("ActivityLog: failed to render key={} (id={}): {}", key, logEntry.getId(), e.getMessage());
            return logEntry.getDescription() != null ? logEntry.getDescription() : key;
        }
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.warn("ActivityLog: failed to serialize args: {}", e.getMessage());
            return null;
        }
    }

    private Object[] deserializeArgs(String json) {
        if (json == null || json.isBlank()) return NO_ARGS;
        try {
            return objectMapper.readValue(json, Object[].class);
        } catch (Exception e) {
            log.warn("ActivityLog: failed to deserialize args '{}': {}", json, e.getMessage());
            return NO_ARGS;
        }
    }
}

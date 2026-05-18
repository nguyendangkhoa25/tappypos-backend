package com.tappy.pos.model.dto.audit;

import com.tappy.pos.model.entity.audit.ActivityLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityLogDTO {
    private Long id;
    private String actorUsername;
    private String actorFullName;
    private String action;
    private String targetType;
    private String targetId;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static ActivityLogDTO from(ActivityLog log) {
        return ActivityLogDTO.builder()
                .id(log.getId())
                .actorUsername(log.getActorUsername())
                .actorFullName(log.getActorFullName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

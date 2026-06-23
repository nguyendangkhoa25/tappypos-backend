package com.tappy.pos.model.dto.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * The {@code description} is rendered in the reader's locale by
 * {@code ActivityLogServiceImpl.toDto(...)} — do not build this DTO from a raw entity description,
 * or i18n is bypassed.
 */
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
}

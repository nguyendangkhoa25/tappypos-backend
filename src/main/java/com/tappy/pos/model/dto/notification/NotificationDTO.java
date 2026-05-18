package com.tappy.pos.model.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String referenceType;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String createdBy;
    private LocalDateTime createdAt;
}

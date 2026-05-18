package com.tappy.pos.model.entity.notification;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.UnifiedTenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends UnifiedTenantEntity {

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    public enum NotificationType {
        SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO, MARKETING, BILLING
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}

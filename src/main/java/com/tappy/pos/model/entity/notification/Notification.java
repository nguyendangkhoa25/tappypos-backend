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

    /**
     * Literal title — set only for user-authored notifications (admin "create") and legacy rows.
     * System notifications leave this null and use {@link #titleKey} + {@link #titleArgs} so the
     * text renders in the reader's locale. See V037__notification_i18n.sql.
     */
    @Column(length = 200)
    private String title;

    /** Literal message — same rule as {@link #title}; system notifications use messageKey/messageArgs. */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** i18n key for the title, e.g. {@code notification.order.new.title}. Rendered at read time. */
    @Column(name = "title_key", length = 150)
    private String titleKey;

    /** JSON array of stringified title arguments. */
    @Column(name = "title_args", columnDefinition = "text")
    private String titleArgs;

    /** i18n key for the message, e.g. {@code notification.order.new.message}. Rendered at read time. */
    @Column(name = "message_key", length = 150)
    private String messageKey;

    /** JSON array of stringified message arguments. */
    @Column(name = "message_args", columnDefinition = "text")
    private String messageArgs;

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

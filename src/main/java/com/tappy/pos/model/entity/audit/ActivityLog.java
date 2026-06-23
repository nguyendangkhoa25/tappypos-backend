package com.tappy.pos.model.entity.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_activity_actor", columnList = "actor_username"),
        @Index(name = "idx_activity_action", columnList = "action"),
        @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "(tenant_id = :tenantFilterId OR tenant_id IS NULL)")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(name = "actor_full_name", length = 100)
    private String actorFullName;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    /**
     * Legacy rendered text. Populated only on pre-V036 rows (and never on new rows, which use
     * {@link #descriptionKey} + {@link #descriptionArgs}). Read path falls back to this when
     * descriptionKey is null. See V036__activity_log_i18n.sql.
     */
    @Column(name = "description", length = 500)
    private String description;

    /** i18n message key, e.g. {@code activity.product.created}. Rendered in the reader's locale on read. */
    @Column(name = "description_key", length = 150)
    private String descriptionKey;

    /** JSON array of stringified message arguments, e.g. {@code ["Áo thun"]}. */
    @Column(name = "description_args", columnDefinition = "text")
    private String descriptionArgs;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

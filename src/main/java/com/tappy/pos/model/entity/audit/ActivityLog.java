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
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId OR tenant_id IS NULL")
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

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

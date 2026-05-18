package com.tappy.pos.model.entity.notification;

import com.tappy.pos.model.entity.UnifiedTenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationPreference extends UnifiedTenantEntity {

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /** "ALL" or comma-separated type names e.g. "ORDER,LOW_STOCK,BILLING" */
    @Column(name = "enabled_types", nullable = false)
    @Builder.Default
    private String enabledTypes = "ALL";
}

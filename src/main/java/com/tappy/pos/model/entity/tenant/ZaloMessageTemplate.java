package com.tappy.pos.model.entity.tenant;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Stores a named Zalo ZNS message template configuration per tenant.
 * Multiple templates per type are allowed; exactly one is marked isDefault.
 * The template_id maps to a pre-approved ZNS template on the Zalo OA portal.
 *
 * Supported template types:
 *   APPOINTMENT_REMINDER — sent ~1 h before a scheduled appointment
 */
@Entity
@Table(name = "zalo_message_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "template_type", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ZaloMessageTemplate extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Discriminator — e.g. "APPOINTMENT_REMINDER". */
    @Column(name = "template_type", nullable = false, length = 50)
    private String templateType;

    /** The Zalo ZNS template ID obtained from the Zalo OA Developer portal. */
    @Column(name = "template_id", nullable = false, length = 100)
    private String templateId;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;
}

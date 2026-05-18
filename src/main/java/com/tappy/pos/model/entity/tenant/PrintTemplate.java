package com.tappy.pos.model.entity.tenant;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Stores a named print template configuration as a JSON blob.
 * Multiple templates per type are allowed; exactly one per type is marked isDefault.
 */
@Entity
@Table(name = "print_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_type", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PrintTemplate extends TenantAwareEntity {

    @Column(name = "template_type", nullable = false, length = 50)
    private String templateType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "config_json", columnDefinition = "TEXT", nullable = false)
    private String configJson;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}

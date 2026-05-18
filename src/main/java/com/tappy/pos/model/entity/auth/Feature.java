package com.tappy.pos.model.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.tappy.pos.model.entity.BaseEntity;

/**
 * Platform-defined feature flags (DASHBOARD, ORDER, POS, etc.).
 * These are master data shared by all tenants — no tenant_id column.
 */
@Entity
@Table(name = "features")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Feature extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public Feature(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.active = true;
    }
}

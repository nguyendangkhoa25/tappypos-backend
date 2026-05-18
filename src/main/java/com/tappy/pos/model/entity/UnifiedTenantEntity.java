package com.tappy.pos.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

/**
 * Base for unified tables (tenant_id nullable):
 *   tenant_id IS NULL  → master record, visible in master context
 *   tenant_id = 'x'   → tenant record, visible only in that tenant's context
 *
 * The null-safe OR condition ensures master rows remain visible when
 * TenantContext is empty (master admin requests).
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId OR tenant_id IS NULL")
public abstract class UnifiedTenantEntity extends BaseEntity {

    @Column(name = "tenant_id")
    private String tenantId;
}

package com.tappy.pos.model.entity.booking;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A bookable physical resource: a bida table, a tennis/sports court, etc.
 * Billed by {@link #hourlyRate} when a booking on it is checked out.
 */
@Entity
@Table(name = "booking_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookingResource extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** TABLE | COURT | OTHER — free-form label for the kind of resource. */
    @Builder.Default
    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType = "TABLE";

    @Builder.Default
    @Column(name = "hourly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    /** ACTIVE | INACTIVE | MAINTENANCE */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
}

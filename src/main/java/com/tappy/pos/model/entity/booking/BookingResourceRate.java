package com.tappy.pos.model.entity.booking;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * A peak/off-peak rate window (giá giờ vàng) for a {@link BookingResource}. When a
 * booking is checked out, {@code BookingServiceImpl} picks the rate of the window whose
 * [startTime, endTime) contains the session's start time-of-day and whose {@link #dayKind}
 * matches the day; if none matches it falls back to the resource's flat {@code hourlyRate}.
 */
@Entity
@Table(name = "booking_resource_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookingResourceRate extends TenantAwareEntity {

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** ALL | WEEKDAY | WEEKEND — which days this window applies to. */
    @Builder.Default
    @Column(name = "day_kind", nullable = false, length = 10)
    private String dayKind = "ALL";

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
}

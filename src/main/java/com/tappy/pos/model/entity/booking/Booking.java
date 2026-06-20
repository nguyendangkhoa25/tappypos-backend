package com.tappy.pos.model.entity.booking;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A booking of a {@link BookingResource}. Two flavours, distinguished by
 * {@link #bookingType}:
 *  - RESERVATION: scheduled for a future slot (scheduledDate/start/end).
 *    Lifecycle RESERVED → IN_PROGRESS (check-in) → COMPLETED (checkout); or
 *    CANCELLED / NO_SHOW.
 *  - WALK_IN: created already running (startedAt = now, status IN_PROGRESS),
 *    billed by elapsed time on checkout.
 *
 * On checkout a POS order is created and referenced by {@link #linkedOrderId};
 * the time charge is computed as ceil(minutes/60h) × hourlyRate.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Booking extends TenantAwareEntity {

    @Column(name = "booking_number", nullable = false, length = 20)
    private String bookingNumber;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_name", nullable = false, length = 255)
    private String resourceName;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** RESERVATION | WALK_IN */
    @Builder.Default
    @Column(name = "booking_type", nullable = false, length = 20)
    private String bookingType = "WALK_IN";

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalTime scheduledEndTime;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "hourly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "time_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal timeAmount = BigDecimal.ZERO;

    /** RESERVED | IN_PROGRESS | COMPLETED | CANCELLED | NO_SHOW */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "RESERVED";

    /** Đặt cọc giữ sân — deposit collected to hold the reservation; netted at checkout. */
    @Builder.Default
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deposit_paid", nullable = false)
    private boolean depositPaid = false;

    /** Links the materialized rows of a recurring weekly reservation (sân cố định). NULL = one-off. */
    @Column(name = "recurrence_group_id", length = 36)
    private String recurrenceGroupId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "linked_order_id")
    private Long linkedOrderId;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
}

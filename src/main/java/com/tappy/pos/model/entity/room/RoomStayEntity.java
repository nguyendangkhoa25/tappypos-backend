package com.tappy.pos.model.entity.room;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One occupancy session (check-in → check-out). Mirrors the BOOKING session lifecycle.
 * billingMode: NIGHTLY | HOURLY | OVERNIGHT.  status: IN_HOUSE | CHECKED_OUT | CANCELLED.
 */
@Entity
@Table(name = "room_stay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomStayEntity extends TenantAwareEntity {

    @Column(name = "stay_number", nullable = false, length = 20)
    private String stayNumber;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "guest_name", length = 255)
    private String guestName;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "guest_id_number", length = 50)
    private String guestIdNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Builder.Default
    @Column(name = "adults", nullable = false)
    private Integer adults = 1;

    @Builder.Default
    @Column(name = "billing_mode", nullable = false, length = 20)
    private String billingMode = "NIGHTLY";

    @Builder.Default
    @Column(name = "rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    /** Planned arrival for a RESERVED stay; null until check-in is done. */
    @Column(name = "reserved_checkin")
    private LocalDateTime reservedCheckin;

    /** Actual check-in time; null while RESERVED. */
    @Column(name = "checkin_at")
    private LocalDateTime checkinAt;

    @Column(name = "expected_checkout")
    private LocalDateTime expectedCheckout;

    @Column(name = "checkout_at")
    private LocalDateTime checkoutAt;

    @Builder.Default
    @Column(name = "units", nullable = false)
    private Integer units = 1;

    @Builder.Default
    @Column(name = "room_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal roomCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deposit", nullable = false, precision = 15, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "IN_HOUSE";

    @Column(name = "linked_order_id")
    private Long linkedOrderId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 255)
    private String createdBy;
}

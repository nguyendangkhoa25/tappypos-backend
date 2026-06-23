package com.tappy.pos.model.entity.room;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A physical room (the board unit) for lodging shop types. Mirrors BookingResource.
 * status: AVAILABLE | OCCUPIED | RESERVED | DIRTY | OOO (out of order).
 */
@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomEntity extends TenantAwareEntity {

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "room_type", length = 100)
    private String roomType;

    @Column(name = "floor", length = 30)
    private String floor;

    @Builder.Default
    @Column(name = "nightly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal nightlyRate = BigDecimal.ZERO;

    @Column(name = "hourly_rate", precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "overnight_rate", precision = 15, scale = 2)
    private BigDecimal overnightRate;

    @Builder.Default
    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy = 2;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";

    @Column(name = "qr_token", length = 64)
    private String qrToken;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    // ── Housekeeping (4d) ──────────────────────────────────────────────────────
    @Column(name = "assigned_cleaner_id")
    private Long assignedCleanerId;

    @Column(name = "assigned_cleaner_name", length = 255)
    private String assignedCleanerName;

    @Column(name = "cleaning_started_at")
    private java.time.LocalDateTime cleaningStartedAt;

    @Column(name = "cleaned_at")
    private java.time.LocalDateTime cleanedAt;

    /** Auto-assign a QR token for guest in-room ordering when a room is first created. */
    @PrePersist
    protected void onRoomPersist() {
        if (this.qrToken == null) this.qrToken = java.util.UUID.randomUUID().toString();
    }
}

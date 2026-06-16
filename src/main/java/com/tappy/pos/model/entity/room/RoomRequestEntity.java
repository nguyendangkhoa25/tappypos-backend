package com.tappy.pos.model.entity.room;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A guest request sent from the in-room QR page to reception.
 * requestType: SERVICE | CLEANING | SUPPLIES | CHECKOUT | OTHER.
 * status: NEW | IN_PROGRESS | DONE | CANCELLED.
 */
@Entity
@Table(name = "room_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomRequestEntity extends TenantAwareEntity {

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "stay_id")
    private Long stayId;

    @Builder.Default
    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType = "SERVICE";

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "NEW";

    @Column(name = "handled_by", length = 255)
    private String handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}

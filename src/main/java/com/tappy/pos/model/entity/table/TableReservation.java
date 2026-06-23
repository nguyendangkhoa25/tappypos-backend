package com.tappy.pos.model.entity.table;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * An advance reservation of a dine-in {@link ShopTable} (đặt bàn trước). A table can hold many
 * future reservations. Distinct from {@code ShopTable.status = RESERVED}, which is the live
 * single-slot toggle; this is the scheduled date/time/party-size calendar.
 *
 * Lifecycle: RESERVED → SEATED (guest arrived, marks the table RESERVED so staff can open a tab);
 * or CANCELLED / NO_SHOW.
 */
@Entity
@Table(name = "table_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TableReservation extends TenantAwareEntity {

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    /** Snapshot of the table number at reservation time (survives table renames). */
    @Column(name = "table_label", nullable = false, length = 100)
    private String tableLabel;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Builder.Default
    @Column(name = "party_size", nullable = false)
    private Integer partySize = 2;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** RESERVED | SEATED | CANCELLED | NO_SHOW */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "RESERVED";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;
}

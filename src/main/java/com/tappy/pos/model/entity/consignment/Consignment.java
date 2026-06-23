package com.tappy.pos.model.entity.consignment;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.ConsignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A consignment (ký gửi) placement header — a publisher/NCC places stock at the shop
 * and is paid only for units that sell. Each line ({@link ConsignmentItem}) is a
 * consigned title linked to an ordinary Product, so sales are counted passively from
 * order_items at settlement time.
 */
@Entity
@Table(name = "consignment", indexes = {
        @Index(name = "idx_consignment_status_e", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Consignment extends TenantAwareEntity {

    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "publisher_name", nullable = false, length = 255)
    private String publisherName;

    @Column(name = "placement_number", nullable = false, length = 30)
    private String placementNumber;

    @Column(name = "placement_date", nullable = false)
    private LocalDate placementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsignmentStatus status = ConsignmentStatus.ACTIVE;

    @Column(length = 500)
    private String note;

    @Column(name = "settled_from")
    private LocalDate settledFrom;

    @Column(name = "settled_to")
    private LocalDate settledTo;

    @Column(name = "settled_date")
    private LocalDateTime settledDate;

    @Column(name = "settled_amount", precision = 15, scale = 2)
    private BigDecimal settledAmount;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "consignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsignmentItem> items = new ArrayList<>();
}

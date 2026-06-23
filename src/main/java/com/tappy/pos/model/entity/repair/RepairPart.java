package com.tappy.pos.model.entity.repair;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * RepairPart — a spare-part / linh kiện line on a {@link RepairTicket}.
 * Carries its own tenant_id (RLS-scoped) and is cascaded from the parent ticket.
 */
@Entity
@Table(name = "repair_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_ticket_id", nullable = false)
    private RepairTicket repairTicket;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}

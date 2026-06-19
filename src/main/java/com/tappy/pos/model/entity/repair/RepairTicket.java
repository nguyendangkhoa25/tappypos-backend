package com.tappy.pos.model.entity.repair;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RepairTicket (Phiếu sửa chữa) — a device repair / service ticket for the
 * ELECTRONICS vertical. Captures the customer, the device (brand/model/serial-IMEI),
 * the reported fault, diagnosis, quote, assigned technician, parts + labor, and
 * warranty-on-repair, with a status flow:
 * RECEIVED → DIAGNOSING → QUOTED → REPAIRING → COMPLETED → DELIVERED (+ CANCELLED).
 */
@Entity
@Table(name = "repair_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RepairTicket extends TenantAwareEntity {

    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber;

    // ── Customer ──
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    // ── Device ──
    @Column(name = "device_type", length = 100)
    private String deviceType;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_imei", length = 100)
    private String serialImei;

    // ── Fault / diagnosis / quote ──
    @Column(name = "reported_fault", columnDefinition = "TEXT", nullable = false)
    private String reportedFault;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Builder.Default
    @Column(name = "quote_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal quoteAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "parts_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal partsAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "labor_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal laborAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "warranty_days", nullable = false)
    private Integer warrantyDays = 0;

    // ── Assignment + workflow ──
    @Column(name = "assigned_technician_id")
    private Long assignedTechnicianId;

    @Column(name = "assigned_technician_name", length = 255)
    private String assignedTechnicianName;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "RECEIVED";

    @Builder.Default
    @Column(name = "is_warranty_claim", nullable = false)
    private Boolean isWarrantyClaim = false;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "linked_order_id")
    private Long linkedOrderId;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "repairTicket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RepairPart> parts = new ArrayList<>();
}

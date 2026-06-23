package com.tappy.pos.model.entity.vehicle;

import com.tappy.pos.model.enums.VehicleUnitStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One physical vehicle unit (chiếc xe) — the SERIAL_NUMBER_MODEL from
 * INVENTORY_IMPLEMENT_GUIDE.md made real. Each row is one titled asset with a unique
 * số khung (frameNo) + số máy (engineNo), its own bảo hành clock and giấy tờ state,
 * linked to its catalog {@code Product}. Tenant-scoped via RLS. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4b.
 */
@Entity
@Table(name = "vehicle_unit")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantFilterId")
public class VehicleUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "frame_no")
    private String frameNo;          // số khung

    @Column(name = "engine_no")
    private String engineNo;         // số máy

    @Column(name = "license_plate")
    private String licensePlate;     // biển số

    @Column(name = "color")
    private String color;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "current_value")
    private BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VehicleUnitStatus status;

    @Column(name = "condition_grade")
    private String conditionGrade;   // Mới / Cũ

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "warranty_exp")
    private LocalDate warrantyExp;

    @Column(name = "paperwork_status")
    private String paperworkStatus;  // Đủ / Thiếu / Đang sang tên

    @Column(name = "sold_to")
    private Long soldTo;             // customer_id

    @Column(name = "sold_to_name")
    private String soldToName;

    @Column(name = "sold_order_id")
    private Long soldOrderId;

    @Column(name = "sold_date")
    private LocalDateTime soldDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

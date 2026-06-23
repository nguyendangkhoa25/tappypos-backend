package com.tappy.pos.model.entity.tradein;

import com.tappy.pos.model.enums.TradeInMode;
import com.tappy.pos.model.enums.TradeInStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A trade-in (thu cũ đổi mới / mua xe cũ): values a used vehicle from a seller and either nets it
 * against a new-vehicle sale (NETTED) or buys it outright (STANDALONE). On completion it spawns a
 * resale Product + a TRADED_IN vehicle_unit. Tenant-scoped via RLS. VEHICLE_SHOP_SHOP_TYPE_PLAN §4c.
 */
@Entity
@Table(name = "trade_in")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantFilterId")
public class TradeInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "trade_in_number", nullable = false)
    private String tradeInNumber;

    // seller
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(name = "seller_name")
    private String sellerName;
    @Column(name = "seller_phone")
    private String sellerPhone;
    @Column(name = "seller_id_number")
    private String sellerIdNumber;   // CCCD

    // incoming used vehicle
    @Column(name = "vehicle_type")
    private String vehicleType;      // MOTORBIKE / E_BIKE / BICYCLE
    @Column(name = "brand")
    private String brand;
    @Column(name = "model")
    private String model;
    @Column(name = "year")
    private Integer year;
    @Column(name = "frame_no")
    private String frameNo;
    @Column(name = "engine_no")
    private String engineNo;
    @Column(name = "license_plate")
    private String licensePlate;
    @Column(name = "color")
    private String color;
    @Column(name = "odometer_km")
    private Integer odometerKm;
    @Column(name = "condition_notes")
    private String conditionNotes;

    @Column(name = "trade_value", nullable = false)
    private BigDecimal tradeValue;   // giá thu xe cũ

    // settlement
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private TradeInMode mode;
    @Column(name = "new_sale_order_id")
    private Long newSaleOrderId;
    @Column(name = "new_price")
    private BigDecimal newPrice;
    @Column(name = "net_amount")
    private BigDecimal netAmount;    // new_price − trade_value

    // resale linkage
    @Column(name = "resale_product_id")
    private Long resaleProductId;
    @Column(name = "resale_unit_id")
    private Long resaleUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeInStatus status;
    @Column(name = "canceled_reason")
    private String canceledReason;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
    @Column(name = "created_by", nullable = false)
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

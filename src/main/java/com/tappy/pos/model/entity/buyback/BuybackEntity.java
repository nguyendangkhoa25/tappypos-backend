package com.tappy.pos.model.entity.buyback;

import com.tappy.pos.model.enums.BuybackStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A buyback — the shop buys a used item outright (no loan/interest), then resells it.
 * Distinct from {@code pawn}. Tenant-scoped via RLS. See PAWN_BUYBACK_SPEC.
 */
@Entity
@Table(name = "buyback")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantFilterId")
public class BuybackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buyback_id")
    private Long buybackId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "item_description")
    private String itemDescription;

    @Column(name = "item_category")
    private String itemCategory;

    @Column(name = "acquisition_price")
    private BigDecimal acquisitionPrice;

    @Column(name = "resale_price")
    private BigDecimal resalePrice;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BuybackStatus status;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    @Column(name = "sold_date")
    private LocalDateTime soldDate;

    @Column(name = "canceled_reason")
    private String canceledReason;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

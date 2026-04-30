package com.knp.model.entity.buyback;

import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "buyback_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuybackOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyback_order_id", nullable = false)
    private BuybackOrder buybackOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private ItemType itemType;

    // ── BUY item fields ──────────────────────────────────────────────────────

    @Column(name = "commodity_id")
    private Long commodityId;

    @Column(name = "commodity_name", length = 100)
    private String commodityName;

    @Column(length = 20)
    private String unit;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 20)
    private ItemCondition conditionType;

    @Column(name = "price_per_unit", precision = 15, scale = 2)
    private BigDecimal pricePerUnit;

    // ── SALE item fields (EXCHANGE type only) ────────────────────────────────

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    // ── Common ───────────────────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    public enum ItemType      { BUY, SALE }
    public enum ItemCondition { NEW, USED, SCRAP }
}

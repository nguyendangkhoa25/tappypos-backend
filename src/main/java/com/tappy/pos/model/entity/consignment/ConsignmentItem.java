package com.tappy.pos.model.entity.consignment;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * One consigned title within a {@link Consignment}. Linked to an ordinary Product so
 * units sold are counted from order_items at settlement; {@code unitPrice} is the amount
 * the shop owes the publisher per unit sold (giá ký gửi).
 */
@Entity
@Table(name = "consignment_item", indexes = {
        @Index(name = "idx_consignment_item_parent_e", columnList = "consignment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConsignmentItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id", nullable = false)
    private Consignment consignment;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "quantity_placed", nullable = false)
    @Builder.Default
    private Integer quantityPlaced = 0;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}

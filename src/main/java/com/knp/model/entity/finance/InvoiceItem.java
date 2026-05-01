package com.knp.model.entity.finance;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class InvoiceItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", precision = 10, scale = 3)
    private BigDecimal quantity;

    @Builder.Default
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_amount_without_tax", precision = 15, scale = 2)
    private BigDecimal totalAmountWithoutTax;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount_with_tax", precision = 15, scale = 2)
    private BigDecimal totalAmountWithTax;
}

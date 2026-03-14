package com.knp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PoStatus status = PoStatus.DRAFT;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public enum PoStatus {
        DRAFT,
        ORDERED,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CANCELLED
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(PurchaseOrderItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

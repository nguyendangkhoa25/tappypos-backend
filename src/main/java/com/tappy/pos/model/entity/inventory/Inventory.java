package com.tappy.pos.model.entity.inventory;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.product.ProductVariant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Inventory extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Builder.Default
    @NotNull(message = "Quantity in stock is required")
    @Column(name = "quantity_in_stock", nullable = false)
    private Long quantityInStock = 0L;

    @Builder.Default
    @NotNull(message = "Reorder level is required")
    @Column(name = "reorder_level", nullable = false)
    private Long reorderLevel = 10L;

    @Builder.Default
    @NotNull(message = "Reorder quantity is required")
    @Column(name = "reorder_quantity", nullable = false)
    private Long reorderQuantity = 50L;

    @Builder.Default
    @NotNull(message = "Unit cost is required")
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @NotBlank(message = "Location/Warehouse is required")
    @Column(name = "warehouse_location", nullable = false, length = 255)
    private String warehouseLocation;

    /** Khu vực / Kho (e.g. A, MAIN, COLD) */
    @Column(name = "zone", length = 50)
    private String zone;

    /** Hàng (e.g. 1, 2, 3) */
    @Column(name = "aisle", length = 20)
    private String aisle;

    /** Kệ (e.g. A, B, C) */
    @Column(name = "shelf", length = 20)
    private String shelf;

    /** Ô / Ngăn (e.g. 01, 02) */
    @Column(name = "bin", length = 20)
    private String bin;

    @Column(name = "last_restock_date")
    private java.time.LocalDateTime lastRestockDate;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "notes", length = 500)
    private String notes;

    @Builder.Default
    @NotNull(message = "Status is required")
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryStatus status = InventoryStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type", nullable = false)
    private InventoryType inventoryType = InventoryType.RETAIL;

    public enum InventoryStatus {
        ACTIVE,
        INACTIVE,
        DISCONTINUED
    }

    public enum InventoryType {
        RETAIL,
        WHOLESALE,
        WAREHOUSE
    }

    /**
     * Check if stock is below reorder level
     */
    public boolean isLowStock() {
        return quantityInStock <= reorderLevel;
    }

    /**
     * Check if inventory is expired
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return java.time.LocalDate.now().isAfter(expiryDate);
    }
}


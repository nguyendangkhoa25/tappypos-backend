package com.knp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * InventoryMovement Entity - Tracks all inventory changes
 * Audit trail for stock in/out operations
 */
@Entity
@Table(name = "inventory_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // Type of movement (IN, OUT, ADJUSTMENT, RETURN, DAMAGE, EXPIRED)
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    // Quantity moved
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    // Reference document (PO number, SO number, etc.)
    @Column(length = 100)
    private String referenceNumber;

    // Reference type (PURCHASE_ORDER, SALES_ORDER, INVENTORY_ADJUSTMENT, etc.)
    @Column(length = 50)
    private String referenceType;

    // Who made the movement
    @Column(length = 100)
    private String createdByUser;

    // Reason for movement (for OUT movements)
    @Column(length = 255)
    private String reason;

    // Notes
    @Column(length = 500)
    private String notes;

    // Previous quantity
    @Column(precision = 15, scale = 2)
    private BigDecimal previousQuantity;

    // Quantity after movement
    @Column(precision = 15, scale = 2)
    private BigDecimal newQuantity;

    // Movement date/time
    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    public enum MovementType {
        IN,              // Stock in from purchase
        OUT,             // Stock out from sale
        RETURN,          // Return from customer
        ADJUSTMENT,      // Manual inventory adjustment
        DAMAGE,          // Damaged goods written off
        EXPIRED,         // Expired items written off
        TRANSFER,        // Transfer between locations/stores
        PHYSICAL_COUNT   // Physical inventory count adjustment
    }
}


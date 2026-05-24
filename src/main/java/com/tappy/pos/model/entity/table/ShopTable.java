package com.tappy.pos.model.entity.table;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shop_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShopTable extends TenantAwareEntity {

    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TableStatus status;

    @Column(name = "current_order_id")
    private Long currentOrderId;

    @Column(name = "location", length = 50)
    private String location;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /** Name of the party who reserved this table (null when not reserved). */
    @Column(name = "reserved_for", length = 100)
    private String reservedFor;

    /** Human-readable time string for the reservation, e.g. "19:00". */
    @Column(name = "reserved_time", length = 10)
    private String reservedTime;

    @PrePersist
    protected void onTablePersist() {
        if (this.status == null) this.status = TableStatus.AVAILABLE;
        if (this.capacity == null) this.capacity = 4;
        if (this.displayOrder == null) this.displayOrder = 0;
    }
}

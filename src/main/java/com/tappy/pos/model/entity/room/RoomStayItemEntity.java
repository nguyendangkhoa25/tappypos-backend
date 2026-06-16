package com.tappy.pos.model.entity.room;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * One in-room folio line (item charged to a stay). source: STAFF | QR.
 */
@Entity
@Table(name = "room_stay_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomStayItemEntity extends TenantAwareEntity {

    @Column(name = "stay_id", nullable = false)
    private Long stayId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "source", nullable = false, length = 20)
    private String source = "STAFF";

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 255)
    private String createdBy;
}

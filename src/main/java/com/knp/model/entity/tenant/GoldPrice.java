package com.knp.model.entity.tenant;

import com.knp.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "gold_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GoldPrice extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 20, scale = 0)
    @Builder.Default
    private BigDecimal buy = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 0)
    @Builder.Default
    private BigDecimal sell = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 0)
    @Builder.Default
    private BigDecimal pawn = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 10;

    @Column(length = 500)
    private String note;

    @Column(name = "show_in_board", nullable = false)
    @Builder.Default
    private Boolean showInBoard = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

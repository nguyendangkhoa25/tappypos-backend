package com.knp.model.entity.finance;

import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "market_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPrice extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    @Builder.Default
    @Column(name = "buy_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal buyPrice = BigDecimal.ZERO;

    @Column(name = "sell_price", precision = 15, scale = 2)
    private BigDecimal sellPrice;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 999;
}

package com.tappy.pos.model.entity.marketgold;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_gold_price_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MarketGoldPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ktype", length = 30, nullable = false)
    private String ktype;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "source", length = 20, nullable = false)
    private String source;

    @Column(name = "buy_price", precision = 20, scale = 0)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", precision = 20, scale = 0)
    private BigDecimal sellPrice;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}

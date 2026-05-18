package com.tappy.pos.model.entity.marketgold;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_gold_prices")
@IdClass(MarketGoldPriceId.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MarketGoldPrice {

    @Id
    @Column(name = "ktype", length = 30)
    private String ktype;

    @Id
    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "buy_price", precision = 20, scale = 0)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", precision = 20, scale = 0)
    private BigDecimal sellPrice;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}

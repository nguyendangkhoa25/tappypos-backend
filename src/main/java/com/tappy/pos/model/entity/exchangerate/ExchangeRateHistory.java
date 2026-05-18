package com.tappy.pos.model.entity.exchangerate;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Column(name = "buy_rate", precision = 18, scale = 4)
    private BigDecimal buyRate;

    @Column(name = "transfer_rate", precision = 18, scale = 4)
    private BigDecimal transferRate;

    @Column(name = "sell_rate", precision = 18, scale = 4)
    private BigDecimal sellRate;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}

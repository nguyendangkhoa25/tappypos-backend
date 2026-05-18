package com.tappy.pos.model.dto.exchangerate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExchangeRateResponse(
        String source,
        LocalDateTime fetchedAt,
        List<RateItem> rates
) {
    public record RateItem(
            String currencyCode,
            BigDecimal buyRate,
            BigDecimal transferRate,
            BigDecimal sellRate,
            LocalDateTime fetchedAt
    ) {}
}

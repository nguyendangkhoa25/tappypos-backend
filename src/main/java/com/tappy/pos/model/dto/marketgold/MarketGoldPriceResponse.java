package com.tappy.pos.model.dto.marketgold;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MarketGoldPriceResponse(
        String source,
        LocalDateTime fetchedAt,
        List<PriceItem> prices
) {
    public record PriceItem(
            String ktype,
            String name,
            String source,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            LocalDateTime fetchedAt
    ) {}
}

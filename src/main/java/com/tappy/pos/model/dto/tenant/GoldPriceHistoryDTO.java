package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One snapshot of a shop gold-price row for the shop's own price-history chart. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldPriceHistoryDTO {
    private String code;
    private String label;
    private BigDecimal buy;
    private BigDecimal sell;
    private LocalDateTime recordedAt;
}

package com.tappy.pos.model.dto.marketprice;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketPriceDTO {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Boolean isActive;
    private String notes;
    private Integer sortOrder;
    private LocalDateTime updatedAt;
}

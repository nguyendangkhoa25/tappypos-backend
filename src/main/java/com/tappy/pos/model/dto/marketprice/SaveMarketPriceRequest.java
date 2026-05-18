package com.tappy.pos.model.dto.marketprice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaveMarketPriceRequest {
    @NotBlank private String name;
    @NotBlank private String unit;
    @NotNull  private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Boolean isActive;
    private String notes;
    private Integer sortOrder;
}

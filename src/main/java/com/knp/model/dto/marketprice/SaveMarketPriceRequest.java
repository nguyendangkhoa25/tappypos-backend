package com.knp.model.dto.marketprice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveMarketPriceRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String unit;

    @NotNull
    @PositiveOrZero
    private BigDecimal buyPrice;

    @PositiveOrZero
    private BigDecimal sellPrice;

    private Boolean isActive = true;
    private String notes;
    private Integer sortOrder = 999;
}

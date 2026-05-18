package com.tappy.pos.model.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveProductVariantRequest {

    @NotBlank
    private String sku;

    private String barcode;

    private Map<String, String> variantOptions;

    private BigDecimal priceOverride;

    private BigDecimal costOverride;
}

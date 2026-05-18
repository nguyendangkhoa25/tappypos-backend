package com.tappy.pos.model.dto.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Configures which fields appear on product/inventory stamp labels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StampTemplateConfig {

    @Builder.Default private boolean showShopName = true;
    @Builder.Default private boolean showSku = true;
    @Builder.Default private boolean showPrice = true;
    @Builder.Default private boolean showBarcode = false;

    /** Inventory stamps only */
    @Builder.Default private boolean showLocation = true;
    @Builder.Default private boolean showBatch = true;
    @Builder.Default private boolean showExpiry = true;

    /** Label dimensions in mm */
    @Builder.Default private int labelWidth = 60;
    @Builder.Default private int labelHeight = 38;

    public static StampTemplateConfig defaults() {
        return StampTemplateConfig.builder().build();
    }

    public static StampTemplateConfig inventoryDefaults() {
        return StampTemplateConfig.builder()
                .showPrice(false)
                .build();
    }
}

package com.tappy.pos.model.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarcodeLookupResult {

    public enum Source { SHOP, CATALOG, NONE }

    private Source source;

    /** Populated when source == SHOP */
    private ProductDTO product;

    /** Populated when source == CATALOG */
    private CatalogHint catalog;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CatalogHint {
        private String barcode;
        private String name;
        private String brand;
        private String categoryHint;
        private String unit;
        private String description;
        private String imageUrl;
    }
}

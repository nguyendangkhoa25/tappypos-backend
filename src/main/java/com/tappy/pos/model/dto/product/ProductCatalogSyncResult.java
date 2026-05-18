package com.tappy.pos.model.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalogSyncResult {
    private int inserted;
    private int updated;
    private int skipped;
    private int totalFetched;
    private int pagesProcessed;
    private boolean success;
    private String errorMessage;
}

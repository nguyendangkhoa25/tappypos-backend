package com.tappy.pos.model.dto.stocktake;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One counted product line within a stocktake session.
 */
@Data
@Builder
public class StocktakeCountDTO {
    private Long id;
    private Long sessionId;
    private Long productId;
    private String productName;
    private String sku;
    private String barcode;
    private Long expectedQty;
    private Long countedQty;
    private Long difference;
    private String countedBy;
    private LocalDateTime countedAt;
    private Boolean applied;
    private String note;
}

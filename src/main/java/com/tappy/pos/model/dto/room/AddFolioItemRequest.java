package com.tappy.pos.model.dto.room;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Add an item to a stay's folio. Provide {@code productId} (price looked up from the product)
 * OR a free-text {@code productName} + {@code unitPrice}.
 */
@Data
public class AddFolioItemRequest {
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    @Min(1)
    private Integer quantity = 1;
    private String note;
    /** STAFF (default) | QR */
    private String source;
}

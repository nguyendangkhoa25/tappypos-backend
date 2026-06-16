package com.tappy.pos.model.dto.stocktake;

import lombok.Data;

/**
 * Request to start a stocktake session. Both fields optional.
 */
@Data
public class CreateStocktakeSessionRequest {
    /** Optional label, e.g. "Kiểm kho cuối tháng 6". */
    private String name;
    private String note;
}

package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Merge one table's running tab into another (gộp bill). The source order's items are folded
 * into the target order (the path variable), the source order is voided, and the source table
 * is released. Used when two tables combine onto a single bill.
 */
@Data
public class MergeBillRequest {

    /** The order whose items are folded into the target order, then voided. */
    @NotNull
    private Long sourceOrderId;
}

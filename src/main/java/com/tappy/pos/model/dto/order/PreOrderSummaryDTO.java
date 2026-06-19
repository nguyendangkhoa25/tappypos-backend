package com.tappy.pos.model.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Dashboard summary for pre-orders (đơn đặt): money held as deposits + upcoming pickups. */
@Data
@Builder
public class PreOrderSummaryDTO {
    private BigDecimal depositsHeld;   // sum of deposits over PENDING pre-orders (tiền cọc đang giữ)
    private long pendingCount;         // open pre-orders
    private long todayCount;           // pre-orders due to be picked up today
}

package com.tappy.pos.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Revenue and order count for one fulfilment channel (DINE_IN / TAKEAWAY / DELIVERY) over a window. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelRevenueDTO {
    private String channel;
    private long orderCount;
    private BigDecimal revenue;
}

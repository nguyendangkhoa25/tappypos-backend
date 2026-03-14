package com.knp.model.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MyWorkStatsDTO {
    private long pendingCount;
    private long completedCount;
    private BigDecimal completedRevenue;
}

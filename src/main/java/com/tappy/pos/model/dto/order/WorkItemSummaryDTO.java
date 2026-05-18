package com.tappy.pos.model.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkItemSummaryDTO {

    private long completedCount;
    private BigDecimal totalRevenue;
    private long totalDurationMinutes;
    private BigDecimal totalCommission;
}

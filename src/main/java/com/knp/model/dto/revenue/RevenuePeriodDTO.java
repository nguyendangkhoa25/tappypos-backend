package com.knp.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenuePeriodDTO {
    private String label;       // "Th1", "01/04", etc.
    private Integer period;     // month (1-12) or day (1-31)
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private Long orderCount;
    private BigDecimal expenses;
    private BigDecimal netProfit;
}

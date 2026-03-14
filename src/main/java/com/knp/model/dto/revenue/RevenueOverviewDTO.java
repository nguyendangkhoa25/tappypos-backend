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
public class RevenueOverviewDTO {
    // All-time
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal profitMarginPercent;
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private Long totalOrders;
    private BigDecimal avgOrderValue;

    // Current month
    private BigDecimal monthRevenue;
    private BigDecimal monthCost;
    private BigDecimal monthProfit;
    private Long monthOrders;

    // Current year
    private BigDecimal yearRevenue;
    private BigDecimal yearCost;
    private BigDecimal yearProfit;
    private Long yearOrders;

    // Context
    private Integer currentMonth;
    private Integer currentYear;
}

package com.barbershop.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueDTO {
    private Long id;
    private Integer year;
    private Integer month;
    private BigDecimal grossRevenue;
    private BigDecimal totalEmployeeSalary;
    private BigDecimal otherCosts;
    private BigDecimal totalCosts;
    private BigDecimal netRevenue;
    private String notes;
    private List<RevenueCostDTO> revenueCosts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

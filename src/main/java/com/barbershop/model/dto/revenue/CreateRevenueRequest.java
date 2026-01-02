package com.barbershop.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRevenueRequest {
    private Integer year;
    private Integer month;
    private List<OtherCostInput> otherCosts; // List of other cost items
    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OtherCostInput {
        private String description;
        private BigDecimal amount;
    }
}


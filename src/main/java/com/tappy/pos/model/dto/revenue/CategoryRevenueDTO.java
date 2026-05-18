package com.tappy.pos.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRevenueDTO {
    private String categoryName;
    private long orderCount;
    private BigDecimal revenue;
    private double percentage;
}

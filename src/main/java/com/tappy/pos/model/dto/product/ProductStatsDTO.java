package com.tappy.pos.model.dto.product;

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
public class ProductStatsDTO {

    /** Total completed orders containing this product in the requested period. */
    private long orderCount;

    /** Total units sold in the requested period. */
    private long qtySold;

    /** Total revenue from this product in the requested period. */
    private BigDecimal revenue;

    /** Timestamp of the most recent completed order that contained this product. */
    private LocalDateTime lastSoldAt;

    /** Revenue from this product in the current calendar month. */
    private BigDecimal revenueThisMonth;

    /** Revenue from this product in the previous calendar month. */
    private BigDecimal revenueLastMonth;

    /** Top 3 customers who ordered this product most (by order count) in the period. */
    private List<TopBuyerDTO> topCustomers;

    /** Top 3 employees who performed / sold this product most (by order count) in the period. */
    private List<TopEmployeeDTO> topEmployees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopBuyerDTO {
        private String name;
        private long orderCount;
        private BigDecimal totalSpend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopEmployeeDTO {
        private String name;
        private long orderCount;
    }
}

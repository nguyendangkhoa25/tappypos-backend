package com.knp.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for dashboard summary statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long totalEmployees;
    private Long activeEmployees;
    private Long inactiveEmployees;
    private Long totalCustomers;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal yearlyRevenue;
    private List<RecentOrderDTO> recentOrders;
    private List<TopEmployeeDTO> topEmployees;
    private List<TopCustomerDTO> topCustomers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDTO {
        private Long id;
        private String customerName;
        private String assignedEmployeeName;
        private BigDecimal totalAmount;
        private String completedAt;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEmployeeDTO {
        private Long id;
        private String name;
        private Long orderCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomerDTO {
        private Long id;
        private String name;
        private String phone;
        private Long orderCount;
        private BigDecimal totalSpent;
    }
}


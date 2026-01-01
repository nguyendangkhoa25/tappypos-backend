package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryDetailDTO {
    private Long salaryId;
    private Long employeeId;
    private String employeeName;
    private String employeePosition;
    private Integer month;
    private Integer year;
    private BigDecimal baseSalary;
    private BigDecimal netSalary;
    private BigDecimal commissionAmount;
    private BigDecimal deductionAmount;
    private BigDecimal overtimeAmount;
    private BigDecimal bonusAmount;
    private BigDecimal allowanceAmount;
    private String notes;
    private String status;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private LocalDateTime createdAt;
    private List<OrderItemEarningDTO> orderItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEarningDTO {
        private Long orderId;
        private Long orderItemId;
        private String serviceName;
        private BigDecimal amount;
        private BigDecimal commissionAmount;
        private LocalDateTime completedAt;
    }
}


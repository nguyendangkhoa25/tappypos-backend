package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEarningDTO {
    private Long orderId;
    private Long orderItemId;
    private String serviceName;
    private BigDecimal amount;
    private BigDecimal commissionAmount;
    private LocalDateTime completedAt;
    private Boolean salaryCalculated;
    private Long includedInSalaryId;
}


package com.barbershop.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueSummaryOrderDTO {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String status;
    private String createdAt;
    private String completedAt;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal commissionAmount;
    private Integer itemCount;
}


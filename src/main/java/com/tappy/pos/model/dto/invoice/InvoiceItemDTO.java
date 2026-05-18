package com.tappy.pos.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemDTO {
    private Long id;
    private Integer lineNumber;
    private Long orderItemId;
    private String serviceName;
    private String serviceCode;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal discount;
    private BigDecimal totalAmountWithoutTax;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmountWithTax;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


package com.knp.model.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceItemRequest {
    private Long itemId;
    private Long invoiceId;
    private Long lineNumber;
    private Long jewelryId;
    private String itemName;
    private String itemCode;
    private BigDecimal unitPrice;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal discount;
    private BigDecimal itemTotalAmountWithoutTax;
    private BigDecimal taxPercentage;
    private BigDecimal itemTotalAmountWithTax;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

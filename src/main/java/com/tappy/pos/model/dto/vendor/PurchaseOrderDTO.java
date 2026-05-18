package com.tappy.pos.model.dto.vendor;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchaseOrderDTO {
    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private String vendorCode;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate expectedDate;
    private LocalDateTime orderedAt;
    private LocalDateTime receivedAt;
    private String createdBy;
    private String notes;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDTO> items;
}

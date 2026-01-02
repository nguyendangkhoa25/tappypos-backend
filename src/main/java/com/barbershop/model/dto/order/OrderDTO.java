package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerNotes;
    private String customerHairType;
    private String customerPreferredServices;
    private String customerSpecialRequests;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private Long invoiceId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<OrderItemDTO> orderItems;
}


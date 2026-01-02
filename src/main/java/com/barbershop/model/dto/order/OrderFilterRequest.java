package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilterRequest {
    private String keyword; // Search by customer name or phone
    private String status; // Filter by order status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
    private Long employeeId; // Filter by assigned employee
    private BigDecimal minAmount; // Filter by minimum order amount
    private BigDecimal maxAmount; // Filter by maximum order amount
    private LocalDateTime fromDate; // Filter by start date
    private LocalDateTime toDate; // Filter by end date
    private Long customerId; // Filter by specific customer
    private Boolean withoutInvoice; // Filter orders without invoice (true = only orders without invoice)
}


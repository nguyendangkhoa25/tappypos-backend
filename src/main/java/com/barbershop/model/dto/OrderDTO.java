package com.barbershop.model.dto;

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
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private String status;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<OrderItemDTO> orderItems;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private List<CreateOrderItemRequest> orderItems;
    private String notes;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderItemRequest {
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignOrderRequest {
    private Long employeeId;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {
    private Long assignedEmployeeId;
    private String status;
    private String notes;
}


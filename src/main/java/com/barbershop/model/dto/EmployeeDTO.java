package com.barbershop.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String position;
    private LocalDate hireDate;
    private String status;
    private BigDecimal totalEarned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CreateEmployeeRequest {
    private String name;
    private String phone;
    private String email;
    private String position;
    private LocalDate hireDate;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class UpdateEmployeeRequest {
    private String name;
    private String phone;
    private String email;
    private String position;
    private String status;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EmployeeEarningsDTO {
    private Long employeeId;
    private String employeeName;
    private BigDecimal totalEarned;
    private Integer completedOrderCount;
}


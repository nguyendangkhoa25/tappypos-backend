package com.barbershop.model.dto.employee;

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
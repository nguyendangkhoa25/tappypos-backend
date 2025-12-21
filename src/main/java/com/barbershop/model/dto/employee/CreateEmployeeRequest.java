package com.barbershop.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {
    private String name;
    private String phone;
    private String email;
    private String position;
    private LocalDate hireDate;
    private BigDecimal baseSalary;
    private String description;
}

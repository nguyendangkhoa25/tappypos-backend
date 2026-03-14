package com.knp.model.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {
    @NotBlank
    private String fullName;
    private String phone;
    private String email;
    @NotNull
    private String position;
    private String department;
    private LocalDate hireDate;
    private Boolean active = true;
    private BigDecimal baseWage;
    private BigDecimal commissionRate;
    private String notes;
    private String avatar;
    private Long userId;
}

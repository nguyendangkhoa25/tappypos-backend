package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String fullName;
    private String nickName;
    private String phone;
    private String email;
    private String position;
    private String department;
    private LocalDate hireDate;
    private Boolean active;
    private BigDecimal baseWage;
    private BigDecimal commissionRate;
    private String notes;
    private String avatar;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String idCardNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String permanentAddress;
    private LocalDate idCardIssuedDate;
    private String idCardIssuedPlace;
    private String idCardFrontImage;
    private String idCardBackImage;
}

package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @NotBlank(message = "Employee name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, unique = true)
    private String phone;

    @Email(message = "Email should be valid")
    @Column(unique = true)
    private String email;

    @Column(length = 50)
    private String username;

    @Column(nullable = false)
    private String position;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "total_earned")
    private BigDecimal totalEarned;

    @OneToMany(mappedBy = "assignedEmployee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    public enum EmployeeStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE
    }
}


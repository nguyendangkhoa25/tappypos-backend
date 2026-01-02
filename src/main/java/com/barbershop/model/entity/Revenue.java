package com.barbershop.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "revenues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Revenue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal grossRevenue; // Sum of all orders total

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal totalEmployeeSalary; // Sum of all employee salaries

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal otherCosts; // User input - sum of all other costs

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal totalCosts; // totalEmployeeSalary + otherCosts

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal netRevenue; // grossRevenue - totalCosts

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "revenue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RevenueCost> revenueCosts = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean deleted = false;
}


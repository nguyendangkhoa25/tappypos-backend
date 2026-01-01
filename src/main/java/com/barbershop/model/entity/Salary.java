package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salaries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "month", "year", "deleted"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salary extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull(message = "Month is required")
    @Column(nullable = false)
    private Integer month; // 1-12

    @NotNull(message = "Year is required")
    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "overtime", nullable = false, precision = 12, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "bonus", nullable = false, precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalaryStatus status = SalaryStatus.DRAFT;

    public enum SalaryStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED
    }

    @PrePersist
    @PreUpdate
    private void calculateNetSalary() {
        if (this.totalEarnings != null) {
            this.netSalary = this.totalEarnings
                    .subtract(this.deductions != null ? this.deductions : BigDecimal.ZERO)
                    .add(this.overtime != null ? this.overtime : BigDecimal.ZERO)
                    .add(this.bonus != null ? this.bonus : BigDecimal.ZERO);
        }
    }
}


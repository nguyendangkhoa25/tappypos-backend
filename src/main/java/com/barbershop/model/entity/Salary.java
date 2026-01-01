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

    @Column(name = "total_earning", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarning = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "deduction_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Column(name = "overtime_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal overtimeAmount = BigDecimal.ZERO;

    @Column(name = "bonus_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @Column(name = "allowance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal allowanceAmount = BigDecimal.ZERO;


    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalaryStatus status = SalaryStatus.SUBMITTED;

    public enum SalaryStatus {
        SUBMITTED,
        APPROVED,
        REJECTED
    }

    /**
     * Get net salary - totalEarning is now the net salary value
     */
    public BigDecimal getNetSalary() {
        return this.totalEarning != null ? this.totalEarning : BigDecimal.ZERO;
    }
}


package com.tappy.pos.model.entity.employee;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.SalaryStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "salary")
public class Salary extends TenantAwareEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Builder.Default
    @Column(name = "base_wage", precision = 15, scale = 2, nullable = false)
    private BigDecimal baseWage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_commission", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SalaryStatus status = SalaryStatus.DRAFT;

    @Builder.Default
    @Column(name = "advance_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}

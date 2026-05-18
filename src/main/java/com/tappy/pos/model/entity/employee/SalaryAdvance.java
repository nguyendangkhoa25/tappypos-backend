package com.tappy.pos.model.entity.employee;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "salary_advance")
public class SalaryAdvance extends TenantAwareEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "salary_id")
    private Long salaryId;

    @Column(name = "is_deducted", nullable = false)
    private boolean deducted;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}

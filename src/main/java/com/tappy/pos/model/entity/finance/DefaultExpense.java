package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "default_expense", indexes = {
        @Index(name = "idx_default_expense_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DefaultExpense extends TenantAwareEntity {

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    /** Day of month (1–31) to use when cloning into a month, null = 1st. */
    @Column(name = "payment_day")
    private Integer paymentDay;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}

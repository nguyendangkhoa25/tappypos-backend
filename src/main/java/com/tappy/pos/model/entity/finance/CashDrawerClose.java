package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A day's cash-drawer reconciliation (one per business day per shop). */
@Entity
@Table(name = "cash_drawer_close")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CashDrawerClose extends TenantAwareEntity {

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "opening_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal openingAmount;

    @Column(name = "expected_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal expectedAmount;

    @Column(name = "counted_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal countedAmount;

    @Column(name = "difference_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal differenceAmount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}

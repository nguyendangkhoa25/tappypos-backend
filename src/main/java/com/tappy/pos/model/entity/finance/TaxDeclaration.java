package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.BusinessType;
import com.tappy.pos.model.enums.TaxDeclarationStatus;
import com.tappy.pos.model.enums.TaxFormType;
import com.tappy.pos.model.enums.TaxPeriodType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một tờ khai thuế của hộ kinh doanh cho một kỳ (mặc định: quý).
 * Tổng thuế = tổng GTGT + tổng TNCN của các dòng nhóm ngành ({@link TaxDeclarationLine}).
 */
@Entity
@Table(name = "tax_declaration")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaxDeclaration extends TenantAwareEntity {

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private TaxPeriodType periodType = TaxPeriodType.QUARTER;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    /** Quý 1..4 (hoặc tháng 1..12; 0 nếu kỳ năm). */
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 20)
    private BusinessType businessType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false, length = 20)
    private TaxFormType formType = TaxFormType.FORM_01_CNKD;

    @Builder.Default
    @Column(name = "declared_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal declaredRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "auto_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal autoRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_vat", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_pit", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxDeclarationStatus status = TaxDeclarationStatus.DRAFT;

    @Column(name = "gov_ref_number", length = 100)
    private String govRefNumber;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaxDeclarationLine> lines = new ArrayList<>();

    /** Đồng bộ tổng GTGT/TNCN/tổng thuế từ các dòng hiện có. */
    public void recomputeTotals() {
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal pit = BigDecimal.ZERO;
        for (TaxDeclarationLine line : lines) {
            vat = vat.add(line.getVatAmount() != null ? line.getVatAmount() : BigDecimal.ZERO);
            pit = pit.add(line.getPitAmount() != null ? line.getPitAmount() : BigDecimal.ZERO);
        }
        this.totalVat = vat;
        this.totalPit = pit;
        this.totalTax = vat.add(pit);
    }
}

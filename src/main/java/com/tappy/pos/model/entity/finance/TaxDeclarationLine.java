package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Một dòng nhóm ngành trong tờ khai. Tỷ lệ GTGT/TNCN và tên ngành được SNAPSHOT từ
 * {@link TaxRateCatalog} tại thời điểm tạo dòng, để tờ khai cũ không thay đổi khi catalog cập nhật.
 */
@Entity
@Table(name = "tax_declaration_line")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaxDeclarationLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private TaxDeclaration declaration;

    @Column(name = "industry_code", nullable = false, length = 50)
    private String industryCode;

    @Column(name = "industry_name", nullable = false, length = 255)
    private String industryName;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "pit_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal pitRate;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "pit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal pitAmount = BigDecimal.ZERO;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}

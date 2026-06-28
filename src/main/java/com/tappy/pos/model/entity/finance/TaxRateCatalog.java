package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Danh mục tỷ lệ thuế theo nhóm ngành (bảng MASTER, không tenant_id / không RLS).
 *
 * <p>Đây là nguồn sự thật cho tỷ lệ GTGT/TNCN và các ngưỡng. Master admin sửa khi luật thay đổi —
 * KHÔNG hardcode trong code. Khi tạo tờ khai, tỷ lệ được snapshot vào {@link TaxDeclarationLine}.
 */
@Entity
@Table(name = "tax_rate_catalog")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TaxRateCatalog extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    /** Tỷ lệ thuế GTGT, đơn vị phần trăm (1.00 = 1%). */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    /** Tỷ lệ thuế TNCN, đơn vị phần trăm (0.50 = 0.5%). */
    @Column(name = "pit_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal pitRate;

    /** Ngưỡng doanh thu/năm được miễn thuế (cấu hình; null = chưa thiết lập). */
    @Column(name = "exempt_threshold_year", precision = 15, scale = 2)
    private BigDecimal exemptThresholdYear;

    /** Ngưỡng doanh thu chuyển sang mẫu 01/CNKD (cấu hình; null = chưa thiết lập). */
    @Column(name = "form_threshold", precision = 15, scale = 2)
    private BigDecimal formThreshold;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}

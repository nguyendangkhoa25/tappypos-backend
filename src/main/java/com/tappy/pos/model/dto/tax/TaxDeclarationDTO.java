package com.tappy.pos.model.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết một tờ khai thuế. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationDTO {
    private Long id;
    private String periodType;
    private Integer periodYear;
    private Integer periodNumber;
    /** Nhãn kỳ thân thiện, vd "Quý 2/2026". */
    private String periodLabel;
    private String businessType;
    private String formType;
    private BigDecimal declaredRevenue;
    private BigDecimal autoRevenue;
    private BigDecimal totalVat;
    private BigDecimal totalPit;
    private BigDecimal totalTax;
    private String status;
    private String govRefNumber;
    private LocalDateTime submittedAt;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TaxDeclarationLineDTO> lines;
}

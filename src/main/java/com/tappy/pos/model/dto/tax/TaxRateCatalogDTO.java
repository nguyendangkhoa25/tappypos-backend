package com.tappy.pos.model.dto.tax;

import lombok.*;

import java.math.BigDecimal;

/** Nhóm ngành thuế + tỷ lệ (cho dropdown chọn ngành). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRateCatalogDTO {
    private String code;
    private String name;
    private BigDecimal vatRate;
    private BigDecimal pitRate;
    private BigDecimal exemptThresholdYear;
    private BigDecimal formThreshold;
}

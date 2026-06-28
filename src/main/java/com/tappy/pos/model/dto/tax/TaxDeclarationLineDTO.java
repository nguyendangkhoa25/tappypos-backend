package com.tappy.pos.model.dto.tax;

import lombok.*;

import java.math.BigDecimal;

/** Một dòng nhóm ngành trong tờ khai (tỷ lệ đã snapshot). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationLineDTO {
    private Long id;
    private String industryCode;
    private String industryName;
    private BigDecimal revenue;
    private BigDecimal vatRate;
    private BigDecimal pitRate;
    private BigDecimal vatAmount;
    private BigDecimal pitAmount;
}

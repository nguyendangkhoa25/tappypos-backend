package com.tappy.pos.model.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ước tính nhanh thuế của một kỳ (chưa lưu). Dùng cho thẻ "Quý này cần nộp".
 * Doanh thu auto lấy từ POS theo nhóm ngành mặc định của shop.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxEstimateDTO {
    private String periodType;
    private Integer periodYear;
    private Integer periodNumber;
    private String periodLabel;
    private String businessType;
    /** True nếu loại hình chưa được hỗ trợ (ENTERPRISE) — frontend hiện màn "sắp có". */
    private boolean businessTypeSupported;
    private BigDecimal autoRevenue;
    private BigDecimal totalVat;
    private BigDecimal totalPit;
    private BigDecimal totalTax;
    /** True nếu doanh thu dưới ngưỡng miễn thuế (tổng thuế = 0). */
    private boolean exempt;
    /** True nếu shop chưa chọn loại hình / nhóm ngành (cần onboarding trước). */
    private boolean needsSetup;
    private List<TaxDeclarationLineDTO> lines;
    /** Đã có tờ khai cho kỳ này chưa (id nếu có). */
    private Long existingDeclarationId;
}

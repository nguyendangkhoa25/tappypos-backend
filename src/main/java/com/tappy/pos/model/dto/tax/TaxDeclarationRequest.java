package com.tappy.pos.model.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Yêu cầu tạo / cập nhật tờ khai nháp. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationRequest {
    /** Mặc định QUARTER nếu để trống. */
    private String periodType;
    private Integer periodYear;
    private Integer periodNumber;
    private String notes;
    /** Các dòng nhóm ngành + doanh thu chủ shop khai. */
    private List<LineRequest> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineRequest {
        private String industryCode;
        private BigDecimal revenue;
    }
}

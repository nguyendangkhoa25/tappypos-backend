package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * Loại hình kinh doanh của cửa hàng — quyết định quy định thuế áp dụng.
 *
 * <p>Phase 1 chỉ hỗ trợ {@link #HOUSEHOLD} và {@link #PERSONAL} (kê khai theo tỷ lệ GTGT/TNCN).
 * {@link #ENTERPRISE} có sẵn trong model nhưng luồng tính/tạo tờ khai chưa hỗ trợ — chủ shop vẫn
 * chọn được, nhưng module Khai báo thuế hiển thị màn "sắp có" cho loại hình này.
 */
@Getter
public enum BusinessType {
    HOUSEHOLD("Hộ kinh doanh"),
    PERSONAL("Cá nhân kinh doanh"),
    ENTERPRISE("Doanh nghiệp / Công ty");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    /** True nếu loại hình được module Khai báo thuế hỗ trợ tính/tạo tờ khai (Phase 1). */
    public boolean isTaxDeclarationSupported() {
        return this == HOUSEHOLD || this == PERSONAL;
    }
}

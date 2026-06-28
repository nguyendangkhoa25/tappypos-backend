package com.tappy.pos.model.enums;

/**
 * Mẫu tờ khai thuế áp dụng (chọn theo ngưỡng doanh thu, cấu hình ở tax_rate_catalog).
 * FORM_01_CNKD     — Tờ khai thuế đối với hộ/cá nhân kinh doanh (doanh thu trên ngưỡng).
 * FORM_01_TKN_CNKD — Thông báo doanh thu (hộ dưới ngưỡng).
 */
public enum TaxFormType {
    FORM_01_CNKD,
    FORM_01_TKN_CNKD
}

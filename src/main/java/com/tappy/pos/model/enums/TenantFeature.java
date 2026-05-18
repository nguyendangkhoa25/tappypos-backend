package com.tappy.pos.model.enums;

/**
 * TenantFeature - Enum for available features that can be enabled for tenants
 */
public enum TenantFeature {
    ORDERS("Quản lý đơn hàng"),
    CUSTOMERS("Quản lý khách hàng"),
    EMPLOYEES("Quản lý nhân viên"),
    PRODUCTS("Quản lý sản phẩm"),
    SALARIES("Quản lý lương"),
    INVOICES("Quản lý hóa đơn"),
    PROMOTIONS("Quản lý khuyến mãi"),
    REVENUES("Quản lý doanh thu"),
    USERS("Quản lý người dùng"),
    REPORTS("Báo cáo"),
    DASHBOARD("Trang chủ");

    private final String description;

    TenantFeature(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getFeatureName() {
        return this.name();
    }
}


package com.tappy.pos.model.enums;

import lombok.Getter;

@Getter
public enum ExpenseCategory {
    ELECTRICITY("Tiền điện"),
    WATER("Tiền nước"),
    RENT("Tiền thuê mặt bằng"),
    INTERNET("Internet / Truyền hình"),
    PHONE("Tiền điện thoại"),
    SUPPLIES("Vật tư tiêu hao"),
    EQUIPMENT("Thiết bị / Sửa chữa"),
    MARKETING("Quảng cáo / Marketing"),
    SALARY_EXTRA("Lương thêm / Thưởng"),
    TRANSPORT("Vận chuyển / Giao hàng"),
    PACKAGING("Bao bì / Đóng gói"),
    SOFTWARE("Phần mềm / Công nghệ"),
    CLEANING("Vệ sinh / Dọn dẹp"),
    TAX("Thuế & Phí"),
    BANK_FEE("Phí ngân hàng"),
    INSURANCE("Bảo hiểm"),
    MAINTENANCE("Sửa chữa / Bảo trì"),
    FOOD_STAFF("Ăn uống nhân viên"),
    OTHER("Khác");

    private final String displayName;

    ExpenseCategory(String displayName) { this.displayName = displayName; }

}

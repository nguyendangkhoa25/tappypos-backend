package com.knp.model.enums;

import lombok.Getter;

@Getter
public enum ExpenseCategory {
    ELECTRICITY("Tiền điện"),
    WATER("Tiền nước"),
    RENT("Tiền thuê mặt bằng"),
    INTERNET("Internet / Truyền hình"),
    SUPPLIES("Vật tư tiêu hao"),
    EQUIPMENT("Thiết bị / Sửa chữa"),
    MARKETING("Quảng cáo / Marketing"),
    SALARY_EXTRA("Lương thêm / Thưởng"),
    OTHER("Khác");

    private final String displayName;

    ExpenseCategory(String displayName) { this.displayName = displayName; }

}

package com.knp.model.enums;

import lombok.Getter;

@Getter
public enum ShopType {
    JEWELRY("Trang sức / Vàng bạc",         "Jewelry / Gold"),
    PAWN_SHOP("Tiệm cầm đồ",                "Pawn Shop"),
    CONVENIENCE_STORE("Tạp hóa / Bán lẻ tổng hợp",   "Convenience Store / General Retail"),
    PHARMACY("Nhà thuốc / Dược phẩm",       "Pharmacy / Drugstore"),
    ELECTRONICS("Điện tử / Điện máy",       "Electronics"),
    FOOD_BEVERAGE("Thực phẩm / Đồ uống",    "Food & Beverage"),
    FASHION("Thời trang / May mặc",         "Fashion / Clothing"),
    BARBER_SHOP("Tiệm cắt tóc / Salon",     "Barber Shop / Salon"),
    COFFEE_SHOP("Quán cà phê",              "Coffee Shop"),
    RESTAURANT("Nhà hàng / Quán ăn",        "Restaurant / Eatery"),
    OTHER("Khác",                           "Other");

    private final String displayName;
    private final String englishName;

    ShopType(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }

    /** Message key for use with MessageService: {@code shop.type.JEWELRY} etc. */
    public String getMessageKey() { return "shop.type." + this.name(); }
}

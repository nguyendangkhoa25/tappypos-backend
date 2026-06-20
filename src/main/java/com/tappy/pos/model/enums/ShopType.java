package com.tappy.pos.model.enums;

import lombok.Getter;

@Getter
public enum ShopType {
    JEWELRY("Trang sức / Vàng bạc",         "Jewelry / Gold"),
    PAWN_SHOP("Tiệm cầm đồ",                "Pawn Shop"),
    CONVENIENCE_STORE("Tạp hóa / Bán lẻ tổng hợp",   "Convenience Store / General Retail"),
    BUILDING_MATERIALS("Cửa hàng vật liệu xây dựng", "Building Materials Store"),
    PHARMACY("Nhà thuốc / Dược phẩm",       "Pharmacy / Drugstore"),
    ELECTRONICS("Điện tử / Điện máy",       "Electronics"),
    FOOD_BEVERAGE("Thực phẩm / Đồ uống",    "Food & Beverage"),
    BAKERY("Tiệm bánh",                     "Bakery"),
    FASHION("Thời trang / May mặc",         "Fashion / Clothing"),
    BARBER_SHOP("Tiệm cắt tóc / Salon",     "Barber Shop / Salon"),
    BARBER_SHOP_MEN("Tiệm tóc nam / Barber",         "Men's Barber Shop"),
    HAIR_SALON("Salon tóc / Làm tóc",                "Hair Salon"),
    NAIL_SHOP("Tiệm nail / Làm móng",       "Nail Studio"),
    LASH_PMU_STUDIO("Tiệm mi / Xăm thẩm mỹ",        "Lash & PMU Studio"),
    SPA_SHOP("Spa / Thẩm mỹ viện",          "Spa / Beauty Center"),
    MASSAGE_SHOP("Tiệm massage / Xoa bóp",           "Massage Shop"),
    BEAUTY_CLINIC("Thẩm mỹ viện",                    "Beauty Clinic"),
    MAKEUP_STUDIO("Tiệm trang điểm / Studio cô dâu", "Makeup & Bridal Studio"),
    BOOK_STORE("Nhà sách",              "Book store"),
    COFFEE_SHOP("Quán cà phê",              "Coffee Shop"),
    RESTAURANT("Nhà hàng / Quán ăn",        "Restaurant / Eatery"),
    PUB("Quán nhậu",                        "Pub / Beer House"),
    PUB_SEAFOOD("Quán nhậu hải sản",        "Seafood Pub"),
    PUB_GOAT("Quán nhậu chuyên dê",         "Goat Pub"),
    PUB_BEEF("Quán nhậu chuyên bò",         "Beef Pub"),
    BILLIARDS_HALL("Quán bida / Billiards",  "Billiards Hall"),
    SPORT_COURT("Sân tennis / Sân thể thao", "Sports Court"),
    HOTEL("Khách sạn",                       "Hotel"),
    MOTEL("Nhà nghỉ",                        "Motel"),
    HOMESTAY("Homestay / Nhà nghỉ dưỡng",    "Homestay"),
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

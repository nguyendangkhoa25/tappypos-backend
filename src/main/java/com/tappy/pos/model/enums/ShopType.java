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
    // ── F&B food verticals (provisioned like RESTAURANT) ──
    PHO_NOODLE("Quán phở / bún bò",          "Pho / Noodle Soup Shop"),
    BANH_MI("Quán bánh mì",                  "Banh Mi Shop"),
    BANH_CUON("Quán bánh cuốn",              "Banh Cuon Shop"),
    BUN_RIEU("Quán bún / bún riêu",          "Bun / Crab Noodle Shop"),
    OFFICE_RICE("Quán cơm văn phòng",        "Office Rice Eatery"),
    CLAY_POT_RICE("Quán cơm niêu",           "Clay Pot Rice Restaurant"),
    COM_TAM("Quán cơm tấm",                  "Broken Rice Eatery"),
    OC_QUAN("Quán ốc",                       "Snail / Seafood Eatery"),
    CHAO_QUAN("Quán cháo",                   "Congee Shop"),
    XOI_QUAN("Quán xôi",                     "Sticky Rice Shop"),
    BANH_XEO("Quán bánh xèo",                "Banh Xeo Shop"),
    BUN_DAU("Quán bún đậu",                  "Bun Dau Shop"),
    BANH_CANH("Quán bánh canh",              "Banh Canh Shop"),
    AN_VAT("Quán ăn vặt",                    "Street Snack Shop"),
    GRILL_BBQ("Nhà hàng nướng",              "BBQ / Grill Restaurant"),
    HOTPOT("Nhà hàng lẩu",                   "Hotpot Restaurant"),
    FRIED_CHICKEN("Gà rán / Fast food",      "Fried Chicken / Fast Food"),
    VEGETARIAN("Quán chay",                  "Vegetarian Eatery"),
    PIZZA_PASTA("Pizza / Mì Ý",              "Pizza / Pasta"),
    KOREAN("Quán Hàn Quốc",                  "Korean Restaurant"),
    JAPANESE("Quán Nhật / Sushi",            "Japanese / Sushi"),
    // ── F&B drink/dessert verticals (provisioned like COFFEE_SHOP, with modifier engine) ──
    MILK_TEA("Quán trà sữa",                 "Milk Tea Shop"),
    DESSERT_CHE("Quán chè",                  "Che Dessert Shop"),
    JUICE("Quán nước ép / sinh tố",          "Juice / Smoothie Bar"),
    ICE_CREAM("Quán kem",                    "Ice Cream Shop"),
    YOGURT("Quán sữa chua",                  "Yogurt Shop"),
    STREET_TEA("Quán trà chanh",             "Street Tea Shop"),
    BILLIARDS_HALL("Quán bida / Billiards",  "Billiards Hall"),
    SPORT_COURT("Sân tennis / Sân thể thao", "Sports Court"),
    HOTEL("Khách sạn",                       "Hotel"),
    MOTEL("Nhà nghỉ",                        "Motel"),
    HOMESTAY("Homestay / Nhà nghỉ dưỡng",    "Homestay"),
    VEHICLE_SHOP("Mua bán xe máy / xe đạp điện / xe đạp", "Vehicle Shop"),
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

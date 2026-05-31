package com.tappy.pos.model.enums;

public enum PawnCategory {
    GOLD("Vàng / Trang sức"),
    JEWELRY("Trang sức / Kim loại quý"),
    ELECTRONICS("Điện tử"),
    VEHICLE("Phương tiện"),
    MOTORBIKE("Xe máy"),
    CAR("Ô tô"),
    WATCH("Đồng hồ"),
    REAL_ESTATE("Bất động sản"),
    GENERAL("Tài sản khác"),
    OTHER("Khác");

    public final String label;

    PawnCategory(String label) {
        this.label = label;
    }
}

package com.tappy.pos.model.enums;

/** Lifecycle of a trade-in (thu cũ đổi mới / mua xe cũ). See VEHICLE_SHOP_SHOP_TYPE_PLAN §4c. */
public enum TradeInStatus {
    COMPLETED,  // đã thu xe; resale unit/product đã tạo
    CANCELLED
}

package com.tappy.pos.model.enums;

/** How a trade-in settles. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4c / §7-Q2. */
public enum TradeInMode {
    NETTED,      // đổi mới: nét vào đơn bán xe mới (giá xe mới − giá thu = phải trả)
    STANDALONE   // mua đứt: thu xe cũ, trả tiền mặt, không gắn đơn bán mới
}

package com.tappy.pos.model.enums;

/** Lifecycle of one physical vehicle unit (chiếc xe). See VEHICLE_SHOP_SHOP_TYPE_PLAN §4b. */
public enum VehicleUnitStatus {
    IN_STOCK,    // còn trong kho, sẵn sàng bán
    RESERVED,    // đã giữ cho khách
    SOLD,        // đã bán
    TRADED_IN,   // xe cũ vừa thu vào (chờ định giá / lên kệ)
    DAMAGED      // hư hỏng / không bán
}

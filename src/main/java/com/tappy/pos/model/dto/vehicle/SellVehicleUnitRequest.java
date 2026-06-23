package com.tappy.pos.model.dto.vehicle;

import lombok.Data;

/**
 * Mark a unit SOLD at POS checkout. warrantyMonths (if given) overrides the unit's stored value;
 * warranty_exp is computed = soldDate + warrantyMonths. Called by the POS flow after the order
 * is created so the unit carries sold_to / sold_order_id / sold_date.
 */
@Data
public class SellVehicleUnitRequest {
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Integer warrantyMonths;
    private String paperworkStatus;
}

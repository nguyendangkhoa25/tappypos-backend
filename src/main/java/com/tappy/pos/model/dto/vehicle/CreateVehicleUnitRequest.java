package com.tappy.pos.model.dto.vehicle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Register one physical vehicle unit (chiếc) against a catalog product. */
@Data
public class CreateVehicleUnitRequest {

    @NotNull(message = "{error.vehicle_unit.productRequired}")
    private Long productId;

    private String frameNo;          // số khung (unique per tenant)
    private String engineNo;         // số máy (unique per tenant)
    private String licensePlate;
    private String color;
    private Integer odometerKm;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private String conditionGrade;   // Mới / Cũ
    private Integer warrantyMonths;
    private String paperworkStatus;  // Đủ / Thiếu / Đang sang tên
    private String notes;
    private String legacyId;
}

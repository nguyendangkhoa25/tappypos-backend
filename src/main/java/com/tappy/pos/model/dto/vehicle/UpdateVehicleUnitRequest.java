package com.tappy.pos.model.dto.vehicle;

import com.tappy.pos.model.enums.VehicleUnitStatus;
import lombok.Data;

import java.math.BigDecimal;

/** Edit a vehicle unit's identity / condition / paperwork / status (not the sale fields). */
@Data
public class UpdateVehicleUnitRequest {
    private String frameNo;
    private String engineNo;
    private String licensePlate;
    private String color;
    private Integer odometerKm;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private VehicleUnitStatus status;   // e.g. RESERVED / DAMAGED (SOLD set via /sell)
    private String conditionGrade;
    private Integer warrantyMonths;
    private String paperworkStatus;
    private String notes;
}

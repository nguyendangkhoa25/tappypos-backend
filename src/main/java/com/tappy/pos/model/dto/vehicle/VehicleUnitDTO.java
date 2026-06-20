package com.tappy.pos.model.dto.vehicle;

import com.tappy.pos.model.enums.VehicleUnitStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class VehicleUnitDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String frameNo;
    private String engineNo;
    private String licensePlate;
    private String color;
    private Integer odometerKm;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private VehicleUnitStatus status;
    private String conditionGrade;
    private Integer warrantyMonths;
    private LocalDate warrantyExp;
    private Boolean warrantyActive;   // true if warrantyExp >= today
    private String paperworkStatus;
    private Long soldTo;
    private String soldToName;
    private Long soldOrderId;
    private LocalDateTime soldDate;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
}

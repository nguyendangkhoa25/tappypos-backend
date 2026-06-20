package com.tappy.pos.model.dto.tradein;

import com.tappy.pos.model.enums.TradeInMode;
import com.tappy.pos.model.enums.TradeInStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TradeInDTO {
    private Long id;
    private String tradeInNumber;
    private Long sellerId;
    private String sellerName;
    private String sellerPhone;
    private String sellerIdNumber;
    private String vehicleType;
    private String brand;
    private String model;
    private Integer year;
    private String frameNo;
    private String engineNo;
    private String licensePlate;
    private String color;
    private Integer odometerKm;
    private String conditionNotes;
    private BigDecimal tradeValue;
    private TradeInMode mode;
    private Long newSaleOrderId;
    private BigDecimal newPrice;
    private BigDecimal netAmount;
    private Long resaleProductId;
    private Long resaleUnitId;
    private TradeInStatus status;
    private String canceledReason;
    private String createdBy;
    private LocalDateTime createdAt;
}

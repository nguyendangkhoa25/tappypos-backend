package com.tappy.pos.model.dto.tradein;

import com.tappy.pos.model.enums.TradeInMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/** Intake of a used vehicle: seller → vehicle details → valuation → settlement. */
@Data
public class CreateTradeInRequest {

    // seller (null = walk-in)
    private Long sellerId;
    private String sellerName;
    private String sellerPhone;
    private String sellerIdNumber;   // CCCD

    // incoming vehicle
    private String vehicleType;      // MOTORBIKE / E_BIKE / BICYCLE — resolves the resale product type
    private String brand;
    private String model;
    private Integer year;
    private String frameNo;
    private String engineNo;
    private String licensePlate;
    private String color;
    private Integer odometerKm;
    private String conditionNotes;

    @NotNull(message = "{error.trade_in.tradeValueRequired}")
    @PositiveOrZero(message = "{error.trade_in.tradeValueRequired}")
    private BigDecimal tradeValue;   // giá thu

    // settlement; default NETTED
    private TradeInMode mode;
    private Long newSaleOrderId;     // linked new-vehicle sale order (NETTED)
    private BigDecimal newPrice;     // giá xe mới (NETTED)

    /** Optional target resale price for the auto-created resale product; defaults to tradeValue. */
    private BigDecimal resalePrice;
}

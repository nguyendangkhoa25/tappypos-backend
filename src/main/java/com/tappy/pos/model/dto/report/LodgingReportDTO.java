package com.tappy.pos.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lodging analytics over a trailing window, from checked-out stays + the room count.
 * occupancyPct = room-nights sold / (rooms × windowDays); ADR = revenue / nights sold;
 * RevPAR = revenue / (rooms × windowDays); avgLengthOfStay = avg nights per stay.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LodgingReportDTO {
    private int windowDays;
    private long roomCount;
    private long stays;
    private long roomNightsSold;
    private BigDecimal roomRevenue;
    private double occupancyPct;
    private BigDecimal adr;
    private BigDecimal revpar;
    private double avgLengthOfStay;
}

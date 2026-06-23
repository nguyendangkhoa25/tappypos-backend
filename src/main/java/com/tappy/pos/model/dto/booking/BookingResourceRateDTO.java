package com.tappy.pos.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/** A peak/off-peak rate window (giá giờ vàng) on a bookable resource. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResourceRateDTO {
    private Long id;

    /** ALL | WEEKDAY | WEEKEND */
    private String dayKind;

    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal rate;
    private Integer sortOrder;
}

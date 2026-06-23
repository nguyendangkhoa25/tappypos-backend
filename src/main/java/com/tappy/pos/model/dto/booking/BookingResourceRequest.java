package com.tappy.pos.model.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Create / update payload for a bookable resource (table / court). */
@Data
public class BookingResourceRequest {

    @NotBlank
    private String name;

    private String resourceType = "TABLE";

    private BigDecimal hourlyRate = BigDecimal.ZERO;

    /** Giờ tối thiểu — session total floored at this amount (0 = none). */
    private BigDecimal minimumCharge = BigDecimal.ZERO;

    private String status = "ACTIVE";

    private String note;

    private Integer sortOrder = 0;

    /**
     * Peak/off-peak rate windows (giá giờ vàng). When non-null on update, the resource's
     * existing windows are fully replaced by this list; null means "leave windows unchanged".
     */
    private List<BookingResourceRateDTO> rates;
}

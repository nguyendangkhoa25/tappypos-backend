package com.tappy.pos.model.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/** Create / update payload for a bookable resource (table / court). */
@Data
public class BookingResourceRequest {

    @NotBlank
    private String name;

    private String resourceType = "TABLE";

    private BigDecimal hourlyRate = BigDecimal.ZERO;

    private String status = "ACTIVE";

    private String note;

    private Integer sortOrder = 0;
}

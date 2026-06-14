package com.tappy.pos.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResourceDTO {
    private Long id;
    private String name;
    private String resourceType;
    private BigDecimal hourlyRate;
    private String status;
    private String note;
    private Integer sortOrder;

    /** Live status: the running booking on this resource, if any (null when free). */
    private BookingDTO activeBooking;
}

package com.tappy.pos.model.dto.room;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/** Create/update a room. */
@Data
public class CreateRoomRequest {
    @NotBlank
    private String roomNumber;
    private String roomType;
    private String floor;
    private BigDecimal nightlyRate;
    private BigDecimal hourlyRate;
    private BigDecimal overnightRate;
    private Integer maxOccupancy;
    private String note;
    private Integer sortOrder;
}

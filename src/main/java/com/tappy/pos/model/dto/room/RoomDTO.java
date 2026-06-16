package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A room for the board. {@code activeStay} is populated (when OCCUPIED) for the grid.
 */
@Data
@Builder
public class RoomDTO {
    private Long id;
    private String roomNumber;
    private String roomType;
    private String floor;
    private BigDecimal nightlyRate;
    private BigDecimal hourlyRate;
    private BigDecimal overnightRate;
    private Integer maxOccupancy;
    private String status;          // AVAILABLE | OCCUPIED | RESERVED | DIRTY | OOO
    private String qrToken;
    private String note;
    private Integer sortOrder;
    private RoomStayDTO activeStay;  // current in-house stay, if any
}

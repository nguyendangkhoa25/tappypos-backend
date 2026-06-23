package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** A reception-inbox row (guest request). */
@Data
@Builder
public class RoomRequestDTO {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private Long stayId;
    private String requestType;     // SERVICE | CLEANING | SUPPLIES | CHECKOUT | OTHER
    private String message;
    private String status;          // NEW | IN_PROGRESS | DONE | CANCELLED
    private String handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}

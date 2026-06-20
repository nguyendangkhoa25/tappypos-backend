package com.tappy.pos.model.dto.table;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** Create an advance table reservation (đặt bàn trước). */
@Data
public class CreateTableReservationRequest {
    @NotNull
    private Long tableId;
    @NotNull
    private LocalDateTime reservedAt;
    private Integer partySize;
    private String customerName;
    private String customerPhone;
    private Long customerId;
    private String note;
}

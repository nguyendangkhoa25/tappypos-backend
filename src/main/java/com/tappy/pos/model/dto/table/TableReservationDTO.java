package com.tappy.pos.model.dto.table;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TableReservationDTO {
    private Long id;
    private Long tableId;
    private String tableLabel;
    private LocalDateTime reservedAt;
    private Integer partySize;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String status;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}

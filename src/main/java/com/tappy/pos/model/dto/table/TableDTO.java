package com.tappy.pos.model.dto.table;

import com.tappy.pos.model.enums.TableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private TableStatus status;
    private Long currentOrderId;
    private String currentOrderNumber;
    private BigDecimal currentOrderTotal;
    private String location;
    private Integer displayOrder;
    private Long elapsedMinutes;
    /** Name of the party who reserved this table (null if not RESERVED). */
    private String reservedFor;
    /** Human-readable reservation time, e.g. "19:00" (null if not RESERVED). */
    private String reservedTime;
    /** Opaque token embedded in this table's QR code (for building the customer ordering URL). */
    private String qrToken;
}

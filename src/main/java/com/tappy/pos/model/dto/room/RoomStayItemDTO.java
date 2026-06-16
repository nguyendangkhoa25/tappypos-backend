package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** One in-room folio line. */
@Data
@Builder
public class RoomStayItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String source;   // STAFF | QR
    private String note;
}

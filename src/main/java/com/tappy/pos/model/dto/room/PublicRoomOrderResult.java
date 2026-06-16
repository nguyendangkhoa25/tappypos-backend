package com.tappy.pos.model.dto.room;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Confirmation returned to the guest after charging minibar items to the folio. */
@Data
@Builder
public class PublicRoomOrderResult {
    private Long stayId;
    private int itemCount;
    private BigDecimal addedTotal;
}

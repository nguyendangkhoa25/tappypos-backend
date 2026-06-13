package com.tappy.pos.model.dto.qrorder;

import lombok.Builder;
import lombok.Data;

/** Returned when a QR token is resolved, so the customer page can show the shop + table. */
@Data
@Builder
public class PublicTableDTO {
    private String shopName;
    private Long tableId;
    private String tableLabel;
}

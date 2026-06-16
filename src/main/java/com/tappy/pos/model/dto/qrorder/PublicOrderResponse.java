package com.tappy.pos.model.dto.qrorder;

import lombok.Builder;
import lombok.Data;

/** Returned after a customer submits; also used for status polling (SUBMITTED/PENDING/CANCELLED/...). */
@Data
@Builder
public class PublicOrderResponse {
    private Long orderId;
    private String orderNumber;
    private String status;
}

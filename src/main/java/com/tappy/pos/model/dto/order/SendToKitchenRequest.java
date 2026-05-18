package com.tappy.pos.model.dto.order;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendToKitchenRequest {
    private Long tableId;
    private String tableLabel;
    private Long customerId;
    private String customerName;
    private String notes;
}

package com.tappy.pos.model.dto.order;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SendToKitchenRequest {
    private Long tableId;
    private String tableLabel;
    private Long customerId;
    private String customerName;
    private String notes;
    /** Target pickup time for takeaway kitchen tickets (null = dine-in). */
    private LocalDateTime pickupTime;
}

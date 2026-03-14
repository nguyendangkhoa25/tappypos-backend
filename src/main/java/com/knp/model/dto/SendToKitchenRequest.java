package com.knp.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendToKitchenRequest {
    private String tableLabel;
    private Long customerId;
    private String customerName;
    private String notes;
}

package com.knp.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendToKitchenResponse {
    private Long orderId;
    private String orderNumber;
    private String tableLabel;
    private String status;
}

package com.knp.model.dto.buyback;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BuybackOrderDTO {
    private Long id;
    private String orderNumber;
    private String type;
    private String status;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String paymentMethod;
    private BigDecimal buyTotal;
    private BigDecimal saleTotal;
    private BigDecimal netAmount;
    private String notes;
    private String createdBy;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private List<BuybackOrderItemDTO> buyItems;
    private List<BuybackOrderItemDTO> saleItems;
}

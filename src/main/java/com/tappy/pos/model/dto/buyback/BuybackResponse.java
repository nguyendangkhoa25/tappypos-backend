package com.tappy.pos.model.dto.buyback;

import com.tappy.pos.model.enums.BuybackStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A buyback record returned to clients. {@code margin} is derived (resale − acquisition), only when SOLD. */
@Data
@Builder
public class BuybackResponse {
    private Long buybackId;
    private Long customerId;
    private String customerName;
    private String customerIdNumber;   // seller CCCD (KYC), denormalised from the customer
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private BigDecimal acquisitionPrice;
    private BigDecimal resalePrice;
    private BigDecimal margin;          // resalePrice − acquisitionPrice (null until SOLD)
    private BuybackStatus status;
    private Long productId;
    private Long orderId;
    private LocalDateTime purchaseDate;
    private LocalDateTime soldDate;
    private String canceledReason;
    private String createdBy;
    private LocalDateTime createdAt;
}

package com.knp.model.dto.buyback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateBuybackOrderRequest {

    @NotBlank
    private String type;            // BUY | EXCHANGE

    private Long customerId;
    private String customerName;
    private String customerPhone;

    @NotBlank
    private String paymentMethod;   // CASH | TRANSFER

    private String status;          // PENDING | COMPLETED (defaults to PENDING)
    private String notes;

    @NotEmpty
    @Valid
    private List<BuyItemRequest> buyItems;

    private List<SaleItemRequest> saleItems;

    private BigDecimal buyTotal;
    private BigDecimal saleTotal;
    private BigDecimal netAmount;

    @Data
    public static class BuyItemRequest {
        @NotNull
        private Long commodityId;
        private String commodityName;
        private String unit;
        @NotNull @PositiveOrZero
        private BigDecimal weight;
        private BigDecimal pricePerUnit;
        private BigDecimal totalPrice;
        private String condition;   // NEW | USED | SCRAP
        private String notes;
    }

    @Data
    public static class SaleItemRequest {
        @NotBlank
        private String productName;
        @NotNull @PositiveOrZero
        private Integer quantity;
        @NotNull @PositiveOrZero
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

package com.knp.model.dto.vendor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderRequest {
    @NotNull
    private Long vendorId;

    private LocalDate expectedDate;
    private String notes;

    @NotNull
    @Size(min = 1)
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        private Long productId;
        @NotNull
        private String productName;
        private String productSku;
        @NotNull
        private Integer quantityOrdered;
        @NotNull
        private BigDecimal unitCost;
    }
}

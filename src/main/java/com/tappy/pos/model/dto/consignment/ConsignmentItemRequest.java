package com.tappy.pos.model.dto.consignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsignmentItemRequest {

    /** The consigned title as an ordinary Product (optional — falls back to the typed name). */
    private Long productId;

    @NotBlank
    private String productName;

    @NotNull
    @PositiveOrZero
    private Integer quantityPlaced;

    /** Amount owed to the publisher per unit sold (giá ký gửi). */
    @NotNull
    @PositiveOrZero
    private BigDecimal unitPrice;
}

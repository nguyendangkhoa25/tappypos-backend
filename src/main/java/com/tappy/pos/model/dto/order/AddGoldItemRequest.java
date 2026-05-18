package com.tappy.pos.model.dto.order;

import com.tappy.pos.model.entity.order.CartItemEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for POST /carts/{cartId}/gold-items
 * Adds a GOLD_IN (buy from customer) or GOLD_OUT (sell to customer) item
 * to the cart without touching catalog inventory.
 */
@Data
@NoArgsConstructor
public class AddGoldItemRequest {

    @NotNull(message = "Item type must be GOLD_IN or GOLD_OUT")
    private CartItemEntity.ItemType itemType;

    @NotBlank(message = "Gold type is required (e.g. Vàng 24K)")
    private String goldType;

    private String goldBrand;

    @NotNull
    @DecimalMin(value = "0.001", message = "Gold weight must be greater than 0")
    private BigDecimal goldWeight;

    @DecimalMin(value = "0.0", message = "Gem weight must be >= 0")
    private BigDecimal gemWeight;

    /** Labor / processing fee per unit weight (chỉ). */
    @DecimalMin(value = "0.0", message = "Processing price must be >= 0")
    private BigDecimal procPrice;

    /** Price per unit weight for this item (buy price or sell price). */
    @NotNull
    @DecimalMin(value = "0.0", message = "Unit price must be >= 0")
    private BigDecimal unitPrice;

    private String notes;
}

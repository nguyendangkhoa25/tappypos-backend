package com.tappy.pos.model.dto.order;

import com.tappy.pos.model.enums.CartStatus;
import com.tappy.pos.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Cart Request DTO
 * Used for creating/updating cart from frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartRequest {
    
    private String cartId;
    
    private Long customerId;
    
    // Add to cart
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /** Optional staff-override price. When present and > 0, replaces the catalogue price for this line item. */
    @DecimalMin(value = "0.0", message = "Unit price must be >= 0")
    private BigDecimal unitPrice;

    /** Optional sell unit. When it matches the product's alt unit, the line is priced per alt unit
     *  and stock is deducted in the base unit (quantity × factor). Null/base unit = normal line. */
    private String sellUnit;

    /** Quote mode (báo giá): when true, the line may exceed available stock (the insufficient-stock
     *  guard is skipped at add time). Defaults to false → normal stock enforcement. */
    private boolean quote;

    private Map<String, String> variants;
    private Long variantId;

    /** Selected modifier option ids (FnB). Resolved to labels + price deltas at add-to-cart. */
    private java.util.List<Long> modifierOptionIds;

    // Update quantity
    private Long cartItemId;
    
    @Min(value = 0, message = "New quantity must be 0 or greater")
    private Integer newQuantity;
    
    // Apply discount
    private DiscountType discountType;
    
    @DecimalMin(value = "0.0", message = "Discount value must be >= 0")
    private BigDecimal discountValue;
    
    private String discountReason;
    
    /** When set, marks this item as part of a combo (used internally by addComboToCart). */
    private Long comboId;

    // Commission (COMMISSION feature)
    private Long assignedEmployeeId;
    private BigDecimal commissionAmount;

    // Coupon/Promotion
    private String couponCode;
    private String promotionId;
    
    private CartStatus status;
    private String notes;
}


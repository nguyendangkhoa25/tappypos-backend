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

    private Map<String, String> variants;
    private Long variantId;
    
    // Update quantity
    private Long cartItemId;
    
    @Min(value = 0, message = "New quantity must be 0 or greater")
    private Integer newQuantity;
    
    // Apply discount
    private DiscountType discountType;
    
    @DecimalMin(value = "0.0", message = "Discount value must be >= 0")
    private BigDecimal discountValue;
    
    private String discountReason;
    
    // Commission (COMMISSION feature)
    private Long assignedEmployeeId;
    private BigDecimal commissionAmount;

    // Coupon/Promotion
    private String couponCode;
    private String promotionId;
    
    private CartStatus status;
    private String notes;
}


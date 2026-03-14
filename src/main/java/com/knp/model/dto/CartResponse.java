package com.knp.model.dto;

import com.knp.model.enums.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cart Response DTO
 * Returned to frontend for cart display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Long id;
    private String cartId;
    private Long customerId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal total;
    private CartStatus status;
    private List<String> appliedCoupons;
    private List<String> appliedPromotions;
    private String notes;
    private Integer totalItemCount; // Sum of all quantities
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


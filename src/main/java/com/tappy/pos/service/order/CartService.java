package com.tappy.pos.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tappy.pos.model.dto.order.AddGoldItemRequest;
import com.tappy.pos.model.dto.order.CartRequest;
import com.tappy.pos.model.dto.order.CartResponse;
import com.tappy.pos.model.dto.order.CheckoutRequest;
import com.tappy.pos.model.dto.order.CheckoutResponse;
import com.tappy.pos.model.dto.order.SendToKitchenRequest;
import com.tappy.pos.model.dto.order.SendToKitchenResponse;
import com.tappy.pos.model.enums.DiscountType;

import java.math.BigDecimal;

/**
 * Cart Service Interface
 * Defines cart operations for POS system
 */
public interface CartService {
    
    /**
     * Initialize a new cart session
     */
    CartResponse initializeCart();
    
    /**
     * Get cart by cartId
     */
    CartResponse getCart(String cartId);
    
    /**
     * Add product to cart
     * Validates stock, handles duplicates (merges quantity)
     */
    CartResponse addItemToCart(String cartId, CartRequest request) throws JsonProcessingException;

    /**
     * Add a gold item (GOLD_IN or GOLD_OUT) to the cart.
     * Bypasses catalog lookup and inventory checks.
     */
    CartResponse addGoldItem(String cartId, AddGoldItemRequest request);
    
    /**
     * Update cart item quantity
     * Removes item if quantity <= 0
     */
    CartResponse updateCartItemQuantity(String cartId, Long cartItemId, Integer newQuantity);
    
    /**
     * Remove item from cart
     */
    CartResponse removeItemFromCart(String cartId, Long cartItemId);
    
    /**
     * Apply discount to cart item
     * @param discountType AMOUNT or PERCENTAGE
     */
    CartResponse applyItemDiscount(String cartId, Long cartItemId, DiscountType discountType, BigDecimal discountValue, String reason);

    /**
     * Update commission assignment on a cart item.
     * @param assignedEmployeeId employee to assign; null clears the assignment
     * @param commissionAmount override amount; null auto-calculates from employee's rate
     */
    CartResponse updateItemCommission(String cartId, Long cartItemId, Long assignedEmployeeId, BigDecimal commissionAmount);
    
    /**
     * Apply coupon code to cart
     * Validates coupon before applying
     */
    CartResponse applyCoupon(String cartId, String couponCode);
    
    /**
     * Remove coupon from cart
     */
    CartResponse removeCoupon(String cartId, String couponCode);
    
    /**
     * Apply promotion to cart
     */
    CartResponse applyPromotion(String cartId, String promotionId);
    
    /**
     * Set customer for cart
     */
    CartResponse setCartCustomer(String cartId, Long customerId);
    
    /**
     * Clear all items from cart
     */
    CartResponse clearCart(String cartId);
    
    /**
     * Mark cart as abandoned
     * (when user leaves without checkout)
     */
    void abandonCart(String cartId);

    /**
     * Send cart to kitchen: create a PENDING order snapshot without deducting inventory.
     * Cart remains ACTIVE so items can still be modified before final checkout.
     */
    SendToKitchenResponse sendToKitchen(String cartId, SendToKitchenRequest request);

    /**
     * Complete checkout:
     * - Apply order-level discount (optional)
     * - Create Order + OrderItems
     * - Deduct inventory stock
     * - Mark cart as COMPLETED
     * - Cancel pending kitchen order if pendingOrderId is provided
     * - Return receipt data
     */
    CheckoutResponse checkout(String cartId, CheckoutRequest request);
}


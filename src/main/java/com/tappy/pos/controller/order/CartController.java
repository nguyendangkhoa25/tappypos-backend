package com.tappy.pos.controller.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.order.AddGoldItemRequest;
import com.tappy.pos.model.dto.order.CartRequest;
import com.tappy.pos.model.dto.order.CartResponse;
import com.tappy.pos.model.dto.order.CheckoutRequest;
import com.tappy.pos.model.dto.order.CheckoutResponse;
import com.tappy.pos.model.dto.order.SendToKitchenRequest;
import com.tappy.pos.model.dto.order.SendToKitchenResponse;
import com.tappy.pos.model.enums.DiscountType;
import com.tappy.pos.service.order.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import com.tappy.pos.annotation.RequiresFeature;

/**
 * Cart Controller - POS Shopping Cart API
 * 
 * Endpoints:
 * POST   /api/v1/carts                           - Initialize cart
 * GET    /api/v1/carts/{cartId}                  - Get cart
 * POST   /api/v1/carts/{cartId}/items            - Add item to cart
 * PUT    /api/v1/carts/{cartId}/items/{itemId}   - Update item quantity
 * DELETE /api/v1/carts/{cartId}/items/{itemId}   - Remove item
 * POST   /api/v1/carts/{cartId}/discount         - Apply discount
 * POST   /api/v1/carts/{cartId}/coupon           - Apply coupon
 * DELETE /api/v1/carts/{cartId}/coupon/{code}    - Remove coupon
 * POST   /api/v1/carts/{cartId}/promotion        - Apply promotion
 * POST   /api/v1/carts/{cartId}/customer         - Set customer
 * DELETE /api/v1/carts/{cartId}                  - Clear cart
 */
@Slf4j
@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@RequiresFeature("POS")
public class CartController {

    private final CartService cartService;

    /**
     * Initialize a new shopping cart session
     * 
     * POST /api/v1/carts
     * Response: CartResponse with empty items and UUID
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> initializeCart() {
        log.info("POST /api/v1/carts - Initialize new cart");
        CartResponse cart = cartService.initializeCart();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(cart, "Cart initialized successfully"));
    }

    /**
     * Get cart by cartId
     * 
     * GET /api/v1/carts/{cartId}
     * Response: CartResponse with all items and totals
     */
    @GetMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable String cartId) {
        log.info("GET /api/v1/carts/{} - Get cart", cartId);
        CartResponse cart = cartService.getCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully"));
    }

    /**
     * Add item to cart
     * 
     * POST /api/v1/carts/{cartId}/items
     * 
     * Request Body:
     * {
     *   "productId": 123,
     *   "quantity": 2,
     *   "variants": {"size": "M", "color": "Red"}  // optional
     * }
     * 
     * - Validates stock availability
     * - Merges quantity if same product + variants exist
     * - Returns updated cart
     */
    @PostMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
        @PathVariable String cartId,
        @RequestBody @Valid CartRequest request) throws JsonProcessingException {
        
        log.info("POST /api/v1/carts/{}/items - Add item: productId={}, qty={}", 
            cartId, request.getProductId(), request.getQuantity());
        
        CartResponse cart = cartService.addItemToCart(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart successfully"));
    }

    /**
     * Update cart item quantity
     * 
     * PUT /api/v1/carts/{cartId}/items/{itemId}
     * 
     * Request Body:
     * {
     *   "newQuantity": 5
     * }
     * 
     * - Removes item if quantity <= 0
     * - Returns updated cart
     */
    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
        @PathVariable String cartId,
        @PathVariable Long itemId,
        @RequestBody @Valid CartRequest request) {
        
        log.info("PUT /api/v1/carts/{}/items/{} - Update quantity: {}", 
            cartId, itemId, request.getNewQuantity());
        
        CartResponse cart = cartService.updateCartItemQuantity(cartId, itemId, request.getNewQuantity());
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart item quantity updated successfully"));
    }

    /**
     * Remove item from cart
     * 
     * DELETE /api/v1/carts/{cartId}/items/{itemId}
     * Response: Updated cart without the removed item
     */
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
        @PathVariable String cartId,
        @PathVariable Long itemId) {
        
        log.info("DELETE /api/v1/carts/{}/items/{} - Remove item", cartId, itemId);
        
        CartResponse cart = cartService.removeItemFromCart(cartId, itemId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart successfully"));
    }

    /**
     * Apply discount to cart item
     * 
     * POST /api/v1/carts/{cartId}/discount
     * 
     * Request Body:
     * {
     *   "cartItemId": 5,
     *   "discountType": "AMOUNT",  // or "PERCENTAGE"
     *   "discountValue": 10.50,
     *   "discountReason": "Manager approval for customer"
     * }
     * 
     * Returns: Updated cart with discount applied
     */
    @PostMapping("/{cartId}/discount")
    public ResponseEntity<ApiResponse<CartResponse>> applyDiscount(
        @PathVariable String cartId,
        @RequestBody @Valid CartRequest request) {
        
        log.info("POST /api/v1/carts/{}/discount - Apply discount to item: {}", 
            cartId, request.getCartItemId());
        
        CartResponse cart = cartService.applyItemDiscount(
            cartId,
            request.getCartItemId(),
            request.getDiscountType(),
            request.getDiscountValue(),
            request.getDiscountReason()
        );
        return ResponseEntity.ok(ApiResponse.success(cart, "Discount applied successfully"));
    }

    /**
     * Apply coupon code to cart
     * 
     * POST /api/v1/carts/{cartId}/coupon
     * 
     * Request Body:
     * {
     *   "couponCode": "SUMMER20"
     * }
     * 
     * - Validates coupon exists and is valid
     * - Checks minimum purchase requirement
     * - Applies discount to cart
     * Returns: Updated cart with coupon applied
     */
    @PostMapping("/{cartId}/coupon")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
        @PathVariable String cartId,
        @RequestBody @Valid CartRequest request) {
        
        log.info("POST /api/v1/carts/{}/coupon - Apply coupon: {}", 
            cartId, request.getCouponCode());
        
        CartResponse cart = cartService.applyCoupon(cartId, request.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success(cart, "Coupon applied successfully"));
    }

    /**
     * Remove coupon from cart
     * 
     * DELETE /api/v1/carts/{cartId}/coupon/{code}
     * Response: Updated cart without coupon discount
     */
    @DeleteMapping("/{cartId}/coupon/{code}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon(
        @PathVariable String cartId,
        @PathVariable String code) {
        
        log.info("DELETE /api/v1/carts/{}/coupon/{} - Remove coupon", cartId, code);
        
        CartResponse cart = cartService.removeCoupon(cartId, code);
        return ResponseEntity.ok(ApiResponse.success(cart, "Coupon removed successfully"));
    }

    /**
     * Apply promotion to cart
     * 
     * POST /api/v1/carts/{cartId}/promotion
     * 
     * Request Body:
     * {
     *   "promotionId": "PROMO123"
     * }
     * 
     * - Applies automatic promotion (buy X get Y, bundle deals, etc.)
     * Returns: Updated cart with promotion applied
     */
    @PostMapping("/{cartId}/promotion")
    public ResponseEntity<ApiResponse<CartResponse>> applyPromotion(
        @PathVariable String cartId,
        @RequestBody @Valid CartRequest request) {
        
        log.info("POST /api/v1/carts/{}/promotion - Apply promotion: {}", 
            cartId, request.getPromotionId());
        
        CartResponse cart = cartService.applyPromotion(cartId, request.getPromotionId());
        return ResponseEntity.ok(ApiResponse.success(cart, "Promotion applied successfully"));
    }

    /**
     * Associate customer with cart
     * 
     * POST /api/v1/carts/{cartId}/customer
     * 
     * Request Body:
     * {
     *   "customerId": 42
     * }
     * 
     * - Associates customer for loyalty tracking
     * - Applies member pricing (if customer is member)
     * Returns: Updated cart with member pricing applied
     */
    @PostMapping("/{cartId}/customer")
    public ResponseEntity<ApiResponse<CartResponse>> setCartCustomer(
        @PathVariable String cartId,
        @RequestBody @Valid CartRequest request) {
        
        log.info("POST /api/v1/carts/{}/customer - Set customer: {}", 
            cartId, request.getCustomerId());
        
        CartResponse cart = cartService.setCartCustomer(cartId, request.getCustomerId());
        return ResponseEntity.ok(ApiResponse.success(cart, "Customer set successfully"));
    }

    /**
     * Clear all items from cart
     * 
     * DELETE /api/v1/carts/{cartId}
     * Response: Empty cart with zero totals
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@PathVariable String cartId) {
        log.info("DELETE /api/v1/carts/{} - Clear cart", cartId);
        
        CartResponse cart = cartService.clearCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart cleared successfully"));
    }

    /**
     * Add a gold item (GOLD_IN or GOLD_OUT) to the cart.
     * No catalog lookup or inventory check — gold weight + price supplied directly.
     *
     * POST /api/v1/carts/{cartId}/gold-items
     */
    @PostMapping("/{cartId}/gold-items")
    @RequiresFeature("POS")
    public ResponseEntity<ApiResponse<CartResponse>> addGoldItem(
            @PathVariable String cartId,
            @RequestBody @Valid AddGoldItemRequest request) {
        log.info("POST /api/v1/carts/{}/gold-items - type={}", cartId, request.getItemType());
        CartResponse cart = cartService.addGoldItem(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Gold item added to cart successfully"));
    }

    /**
     * Send cart to kitchen (TABLE mode)
     *
     * POST /api/v1/carts/{cartId}/send-to-kitchen
     */
    @PostMapping("/{cartId}/send-to-kitchen")
    public ResponseEntity<ApiResponse<SendToKitchenResponse>> sendToKitchen(
            @PathVariable String cartId,
            @RequestBody SendToKitchenRequest request) {
        log.info("POST /api/v1/carts/{}/send-to-kitchen - tableLabel={}", cartId, request.getTableLabel());
        SendToKitchenResponse response = cartService.sendToKitchen(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Order sent to kitchen"));
    }

    /**
     * Complete checkout
     *
     * POST /api/v1/carts/{cartId}/checkout
     *
     * Request Body (all optional):
     * {
     *   "discountAmount": 50000,
     *   "discountType": "AMOUNT",
     *   "paymentMethod": "CASH",
     *   "amountPaid": 500000
     * }
     */
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @PathVariable String cartId,
            @RequestBody @Valid CheckoutRequest request) {
        log.info("POST /api/v1/carts/{}/checkout", cartId);
        CheckoutResponse response = cartService.checkout(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Checkout completed successfully"));
    }

    /**
     * Update commission assignment on a cart item (COMMISSION feature required)
     *
     * PATCH /api/v1/carts/{cartId}/items/{itemId}/commission
     */
    @PatchMapping("/{cartId}/items/{itemId}/commission")
    @RequiresFeature("COMMISSION")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemCommission(
            @PathVariable String cartId,
            @PathVariable Long itemId,
            @RequestBody CartRequest request) {
        log.info("PATCH /api/v1/carts/{}/items/{}/commission - employee={}", cartId, itemId, request.getAssignedEmployeeId());
        CartResponse cart = cartService.updateItemCommission(cartId, itemId, request.getAssignedEmployeeId(), request.getCommissionAmount());
        return ResponseEntity.ok(ApiResponse.success(cart, "Commission updated"));
    }

    /**
     * Mark cart as abandoned
     *
     * POST /api/v1/carts/{cartId}/abandon
     *
     * Called when user leaves POS without completing checkout
     * Used for analytics and abandoned cart recovery
     */
    @PostMapping("/{cartId}/abandon")
    public ResponseEntity<ApiResponse<Void>> abandonCart(@PathVariable String cartId) {
        log.info("POST /api/v1/carts/{}/abandon - Mark cart as abandoned", cartId);
        
        cartService.abandonCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(null, "Cart marked as abandoned"));
    }
}


package com.knp.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.order.CartItemResponse;
import com.knp.model.dto.order.CartRequest;
import com.knp.model.dto.order.CartResponse;
import com.knp.model.dto.order.CheckoutRequest;
import com.knp.model.dto.order.CheckoutResponse;
import com.knp.model.dto.order.SendToKitchenRequest;
import com.knp.model.dto.order.SendToKitchenResponse;
import com.knp.model.dto.inventory.InventoryDTO;
import com.knp.model.dto.product.ProductDTO;
import com.knp.model.dto.tenant.ShopInfoDTO;
import com.knp.model.entity.order.CartEntity;
import com.knp.model.entity.order.CartItemEntity;
import com.knp.model.entity.order.Order;
import com.knp.model.entity.order.OrderItem;
import com.knp.model.enums.CartStatus;
import com.knp.model.enums.DiscountType;
import com.knp.model.entity.customer.Customer;
import com.knp.repository.order.CartItemRepository;
import com.knp.repository.order.CartRepository;
import com.knp.repository.customer.CustomerRepository;
import com.knp.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import com.knp.service.inventory.InventoryService;
import com.knp.service.product.ProductService;
import com.knp.service.tenant.ShopInfoService;
import com.knp.service.customer.LoyaltyService;

/**
 * Cart Service Implementation
 * Implements cart management operations for POS system
 * <p>
 * Operations:
 * - Initialize new cart
 * - Add/remove/update items
 * - Apply discounts, coupons, promotions
 * - Manage customer association
 * - Calculate totals
 * <p>
 * Performance: All operations < 100ms target
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final ShopInfoService shopInfoService;
    private final PromotionService promotionService;
    private final LoyaltyService loyaltyService;

    /**
     * Initialize a new cart session
     * Creates UUID and returns empty cart
     */
    @Override
    public CartResponse initializeCart() {
        log.info("Initializing new cart");

        CartEntity cart = CartEntity.builder()
                .cartId(UUID.randomUUID().toString())
                .status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .appliedCoupons("[]")
                .appliedPromotions("[]")
                .build();

        CartEntity saved = cartRepository.save(cart);
        log.info("Cart initialized with ID: {}", saved.getCartId());
        return mapToResponse(saved);
    }

    /**
     * Get cart by cartId
     */
    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String cartId) {
        log.debug("Getting cart: {}", cartId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> {
                    log.warn("Cart not found: {}", cartId);
                    return new ResourceNotFoundException(messageService.getMessage("cart.not.found"));
                });

        return mapToResponse(cart);
    }

    /**
     * Add item to cart with stock validation and duplicate handling
     * If same product + variants exist, merge quantity
     * <p>
     * 1. Validate quantity
     * 2. Get product details from ProductService
     * 3. Check stock availability via InventoryService
     * 4. Check for duplicate items (merge if exists)
     * 5. Add or merge item
     * 6. Recalculate totals
     */
    @Override
    public CartResponse addItemToCart(String cartId, CartRequest request) throws JsonProcessingException {
        log.info("Adding item to cart {}: product={}, qty={}",
                cartId, request.getProductId(), request.getQuantity());

        // Get cart
        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        // Step 1: Validate quantity
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException(messageService.getMessage("validation.invalid.quantity"));
        }

        // Step 2: Get product details
        ProductDTO product;
        try {
            product = productService.getProductById(request.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        messageService.getMessage("product.not.found")
                );
            }
        } catch (Exception e) {
            log.warn("Failed to get product details for product ID: {}", request.getProductId(), e);
            throw new BadRequestException(
                    messageService.getMessage("product.not.found")
            );
        }

        // Step 3: Check stock availability via InventoryService
        BigDecimal resolvedUnitCost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
        try {
            // Get inventory for this product
            // Using Pageable with size=1 to get first available inventory
            var inventoryPage = inventoryService.getInventoryByProductId(
                    request.getProductId(),
                    org.springframework.data.domain.PageRequest.of(0, 1)
            );

            if (inventoryPage.isEmpty()) {
                log.warn("No inventory found for product: {}", request.getProductId());
                throw new BadRequestException(
                        messageService.getMessage("product.out.of.stock")
                );
            }

            InventoryDTO inventory = inventoryPage.getContent().get(0);

            // Capture unit cost from inventory (overrides product cost_price)
            if (inventory.getUnitCost() != null && inventory.getUnitCost().compareTo(BigDecimal.ZERO) > 0) {
                resolvedUnitCost = inventory.getUnitCost();
            }

            // Validate stock availability
            if (inventory.getQuantityInStock() == null || inventory.getQuantityInStock() < request.getQuantity()) {
                Long available = inventory.getQuantityInStock() != null ?
                        inventory.getQuantityInStock() : 0;

                log.warn("Insufficient stock for product {}: requested={}, available={}",
                        request.getProductId(), request.getQuantity(), available);

                throw new BadRequestException(
                        String.format(
                                messageService.getMessage("product.insufficient.stock"),
                                product.getName(),
                                available
                        )
                );
            }

            log.debug("Stock validation passed. Product: {}, Stock: {}, UnitCost: {}",
                    product.getName(), inventory.getQuantityInStock(), resolvedUnitCost);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking stock for product {}: {}", request.getProductId(), e.getMessage(), e);
            throw new BadRequestException(
                    messageService.getMessage("product.stock.check.failed")
            );
        }

        // Step 4 & 5: Check for duplicate item and add or merge
        String variantsJson = request.getVariants() != null ?
                objectMapper.writeValueAsString(request.getVariants()) : "{}";

        List<CartItemEntity> duplicates = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId())
                        && item.getVariants().equals(variantsJson))
                .toList();

        if (!duplicates.isEmpty()) {
            // Merge quantity with existing item
            CartItemEntity existing = duplicates.getFirst();
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.recalculateLineTotal();
            log.debug("Merged quantity for existing item: {} (new qty: {})",
                    existing.getId(), existing.getQuantity());
        } else {
            // Add new item with product details
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    //Todo to generate barcode
                    //.barcode(product.getBarcode())
                    .quantity(request.getQuantity())
                    .basePrice(product.getPrice())
                    .unitPrice(product.getPrice())
                    .unitCost(resolvedUnitCost)
                    .discountType(DiscountType.NONE)
                    .discountValue(BigDecimal.ZERO)
                    .variants(variantsJson)
                    .build();

            newItem.recalculateLineTotal();
            cart.addItem(newItem);
            log.debug("Added new item to cart: {} - {}", request.getProductId(), product.getName());
        }

        // Step 6: Recalculate cart totals
        cart.recalculateTotals();
        CartEntity saved = cartRepository.save(cart);

        log.info("Item added successfully to cart {}. Cart total: {}", cartId, saved.getTotal());
        return mapToResponse(saved);
    }

    /**
     * Update cart item quantity
     * Removes item if quantity <= 0
     */
    @Override
    public CartResponse updateCartItemQuantity(String cartId, Long cartItemId, Integer newQuantity) {
        log.info("Updating cart item quantity: cartId={}, itemId={}, qty={}",
                cartId, cartItemId, newQuantity);

        // Get cart
        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        // Get item
        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.item.not.found")));

        // Update or remove
        if (newQuantity == null || newQuantity <= 0) {
            cart.removeItem(item);
            log.debug("Removed item from cart");
        } else {
            item.setQuantity(newQuantity);
            item.recalculateLineTotal();
            log.debug("Updated item quantity to: {}", newQuantity);
        }

        // Recalculate cart totals
        cart.recalculateTotals();
        CartEntity saved = cartRepository.save(cart);

        return mapToResponse(saved);
    }

    /**
     * Remove item from cart
     */
    @Override
    public CartResponse removeItemFromCart(String cartId, Long cartItemId) {
        log.info("Removing item from cart: cartId={}, itemId={}", cartId, cartItemId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.item.not.found")));

        cart.removeItem(item);
        cart.recalculateTotals();
        CartEntity saved = cartRepository.save(cart);

        log.info("Item removed successfully");
        return mapToResponse(saved);
    }

    /**
     * Apply discount to cart item
     */
    @Override
    public CartResponse applyItemDiscount(String cartId, Long cartItemId,
                                          DiscountType discountType, BigDecimal discountValue, String reason) {

        log.info("Applying discount to item: cartId={}, itemId={}, type={}, value={}",
                cartId, cartItemId, discountType, discountValue);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.item.not.found")));

        // Apply discount
        item.applyDiscount(discountType, discountValue, reason);

        // Recalculate cart totals
        cart.recalculateTotals();
        CartEntity saved = cartRepository.save(cart);

        log.info("Discount applied successfully");
        return mapToResponse(saved);
    }

    /**
     * Apply coupon code to cart
     * TODO: Validate coupon via CouponService
     */
    @Override
    public CartResponse applyCoupon(String cartId, String couponCode) {
        log.info("Applying coupon to cart: cartId={}, code={}", cartId, couponCode);

        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new BadRequestException(messageService.getMessage("coupon.code.required"));
        }

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        // Parse current coupons
        List<String> coupons = parseCoupons(cart.getAppliedCoupons());

        // Check if already applied
        if (coupons.contains(couponCode)) {
            throw new BadRequestException(messageService.getMessage("coupon.already.applied"));
        }

        // TODO: Validate coupon via CouponService
        // For now, just add it

        coupons.add(couponCode);
        cart.setAppliedCoupons(toJson(coupons));

        // TODO: Apply coupon discount to cart
        // For now, manually subtract discount

        CartEntity saved = cartRepository.save(cart);
        log.info("Coupon applied successfully");
        return mapToResponse(saved);
    }

    /**
     * Remove coupon from cart
     */
    @Override
    public CartResponse removeCoupon(String cartId, String couponCode) {
        log.info("Removing coupon from cart: cartId={}, code={}", cartId, couponCode);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        List<String> coupons = parseCoupons(cart.getAppliedCoupons());
        coupons.remove(couponCode);
        cart.setAppliedCoupons(toJson(coupons));

        // TODO: Reverse coupon discount from cart

        CartEntity saved = cartRepository.save(cart);
        log.info("Coupon removed successfully");
        return mapToResponse(saved);
    }

    /**
     * Apply promotion to cart
     */
    @Override
    public CartResponse applyPromotion(String cartId, String promotionId) {
        log.info("Applying promotion to cart: cartId={}, promotionId={}", cartId, promotionId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        List<String> promotions = parsePromotions(cart.getAppliedPromotions());

        if (!promotions.contains(promotionId)) {
            promotions.add(promotionId);
            cart.setAppliedPromotions(toJson(promotions));
        }

        // TODO: Apply promotion discount

        CartEntity saved = cartRepository.save(cart);
        log.info("Promotion applied successfully");
        return mapToResponse(saved);
    }

    /**
     * Set customer for cart
     */
    @Override
    public CartResponse setCartCustomer(String cartId, Long customerId) {
        log.info("Setting customer for cart: cartId={}, customerId={}", cartId, customerId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        cart.setCustomerId(customerId);

        // TODO: Apply member pricing
        // If customer is member, recalculate prices for all items

        CartEntity saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    /**
     * Clear all items from cart
     */
    @Override
    public CartResponse clearCart(String cartId) {
        log.info("Clearing cart: {}", cartId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        cart.clearItems();
        cart.recalculateTotals();

        CartEntity saved = cartRepository.save(cart);
        log.info("Cart cleared successfully");
        return mapToResponse(saved);
    }

    /**
     * Send cart to kitchen: creates a PENDING order snapshot from the cart.
     * No inventory is deducted. Cart stays ACTIVE so items can still be edited.
     * Calling checkout later will cancel this pending order.
     */
    @Override
    public SendToKitchenResponse sendToKitchen(String cartId, SendToKitchenRequest request) {
        log.info("Send to kitchen requested for cart: {}", cartId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException(messageService.getMessage("cart.empty"));
        }
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BadRequestException(messageService.getMessage("cart.not.active"));
        }

        Customer resolvedCustomer = null;
        String resolvedCustomerName = null;
        if (request.getCustomerId() != null) {
            resolvedCustomer = customerRepository.findByIdActive(request.getCustomerId()).orElse(null);
            if (resolvedCustomer != null) resolvedCustomerName = resolvedCustomer.getName();
        }
        if (resolvedCustomerName == null && request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            resolvedCustomerName = request.getCustomerName().trim();
        }

        String orderNumber = generateOrderNumber();
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(cart.getTotal());
        order.setDiscountAmount(cart.getTotalDiscount() != null ? cart.getTotalDiscount() : BigDecimal.ZERO);
        order.setTaxPercentage(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setNotes(request.getNotes());
        order.setTableLabel(request.getTableLabel());
        order.setSource("POS");
        if (resolvedCustomer != null) order.setCustomer(resolvedCustomer);
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setCreatedBy(currentUser);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemEntity cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductId(cartItem.getProductId());
            oi.setProductName(cartItem.getProductName());
            oi.setQuantity(cartItem.getQuantity());
            oi.setUnitPrice(cartItem.getUnitPrice());
            oi.setUnitCost(cartItem.getUnitCost());
            oi.setStatus(OrderItem.ItemStatus.PENDING);
            orderItems.add(oi);
        }
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        log.info("Kitchen ticket created: {} (tableLabel={})", orderNumber, request.getTableLabel());

        return SendToKitchenResponse.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .tableLabel(savedOrder.getTableLabel())
                .status(savedOrder.getStatus().name())
                .build();
    }

    /**
     * Complete checkout:
     * 1. Validate cart is active and non-empty
     * 2. Apply order-level discount
     * 3. Cancel pending kitchen order (if pendingOrderId is provided)
     * 4. Create Order + OrderItems
     * 5. Deduct inventory stock for each item
     * 6. Mark cart COMPLETED
     * 7. Return receipt data
     */
    @Override
    public CheckoutResponse checkout(String cartId, CheckoutRequest request) {
        log.info("Checkout requested for cart: {}", cartId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException(messageService.getMessage("cart.empty"));
        }
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BadRequestException(messageService.getMessage("cart.not.active"));
        }

        // --- Totals ---
        BigDecimal itemsSubtotal = cart.getSubtotal();
        BigDecimal itemDiscount  = cart.getTotalDiscount();
        BigDecimal orderDiscount = BigDecimal.ZERO;

        if (request.getDiscountAmount() != null
                && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getDiscountType() == DiscountType.PERCENTAGE) {
                orderDiscount = itemsSubtotal.subtract(itemDiscount)
                        .multiply(request.getDiscountAmount())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                orderDiscount = request.getDiscountAmount()
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // --- Promotion discount ---
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        String appliedPromotionCode = null;
        if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
            try {
                promotionDiscount = promotionService.applyAtCheckout(
                        request.getPromotionCode(), itemsSubtotal.subtract(itemDiscount).max(BigDecimal.ZERO));
                appliedPromotionCode = request.getPromotionCode().toUpperCase().trim();
            } catch (Exception e) {
                log.warn("Promotion code {} rejected: {}", request.getPromotionCode(), e.getMessage());
                throw e;
            }
        }

        BigDecimal totalDiscount = itemDiscount.add(orderDiscount).add(promotionDiscount);
        BigDecimal taxableAmount = itemsSubtotal.subtract(totalDiscount).max(BigDecimal.ZERO);
        BigDecimal totalTax      = taxableAmount.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total         = taxableAmount.add(totalTax);

        // --- Loyalty points redemption (applied after tax) ---
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        int loyaltyPointsRedeemed = 0;

        // --- Resolve customer (needed for loyalty) ---
        Long resolvedCustomerId = cart.getCustomerId() != null ? cart.getCustomerId() : request.getCustomerId();
        Customer resolvedCustomer = null;
        String resolvedCustomerName = null;
        if (resolvedCustomerId != null) {
            resolvedCustomer = customerRepository.findByIdActive(resolvedCustomerId).orElse(null);
            if (resolvedCustomer != null) resolvedCustomerName = resolvedCustomer.getName();
        }
        if (resolvedCustomerName == null && request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            resolvedCustomerName = request.getCustomerName().trim();
        }
        if (resolvedCustomer == null) {
            resolvedCustomer = customerRepository.findByName("Khách Lẻ").orElse(null);
            if (resolvedCustomer != null && resolvedCustomerName == null) {
                resolvedCustomerName = resolvedCustomer.getName();
            }
        }

        // --- Loyalty redemption (requires a real customer, applied after other discounts) ---
        if (resolvedCustomer != null && resolvedCustomerId != null
                && request.getLoyaltyPointsToRedeem() != null
                && request.getLoyaltyPointsToRedeem() > 0) {
            try {
                loyaltyDiscount = loyaltyService.redeemPoints(
                        resolvedCustomerId, request.getLoyaltyPointsToRedeem(), null);
                loyaltyPointsRedeemed = request.getLoyaltyPointsToRedeem();
                total = total.subtract(loyaltyDiscount).max(BigDecimal.ZERO);
            } catch (Exception e) {
                log.warn("Loyalty redemption failed for customer {}: {}", resolvedCustomerId, e.getMessage());
                throw e;
            }
        }

        // --- Payment ---
        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod() : "CASH";
        BigDecimal amountPaid    = request.getAmountPaid() != null ? request.getAmountPaid() : total;
        BigDecimal changeAmount  = amountPaid.subtract(total).max(BigDecimal.ZERO);

        // --- Cancel pending kitchen order if provided ---
        if (request.getPendingOrderId() != null) {
            orderRepository.findById(request.getPendingOrderId()).ifPresent(pending -> {
                if (pending.getStatus() == Order.OrderStatus.PENDING) {
                    String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
                    pending.cancel("Replaced by checkout", currentUser);
                    orderRepository.save(pending);
                    log.info("Cancelled pending kitchen order: {}", pending.getOrderNumber());
                }
            });
        }

        // --- Build Order ---
        String orderNumber = generateOrderNumber();
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setTotalAmount(total);
        order.setDiscountAmount(totalDiscount);
        order.setTaxPercentage(new BigDecimal("10.00"));
        order.setTaxAmount(totalTax);
        order.setPaymentMethod(paymentMethod);
        order.setAmountPaid(amountPaid);
        order.setChangeAmount(changeAmount);
        order.setNotes(request.getNotes());
        order.setPromotionCode(appliedPromotionCode);
        order.setPromotionDiscount(promotionDiscount);
        order.setLoyaltyPointsRedeemed(loyaltyPointsRedeemed);
        order.setLoyaltyDiscount(loyaltyDiscount);
        if (resolvedCustomer != null) order.setCustomer(resolvedCustomer);
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setCreatedBy(currentUser);
        order.complete(currentUser);

        // --- Build OrderItems + deduct inventory ---
        List<OrderItem> orderItems = new ArrayList<>();
        List<CheckoutResponse.CheckoutItemSummary> summaries = new ArrayList<>();

        for (CartItemEntity cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductId(cartItem.getProductId());
            oi.setProductName(cartItem.getProductName());
            oi.setQuantity(cartItem.getQuantity());
            oi.setUnitPrice(cartItem.getUnitPrice());
            oi.setUnitCost(cartItem.getUnitCost());
            oi.setStatus(OrderItem.ItemStatus.COMPLETED);
            orderItems.add(oi);

            // Deduct stock — log warning on failure, never abort the sale
            try {
                var inventoryPage = inventoryService.getInventoryByProductId(
                        cartItem.getProductId(),
                        org.springframework.data.domain.PageRequest.of(0, 1));
                if (!inventoryPage.isEmpty()) {
                    Long inventoryId = inventoryPage.getContent().get(0).getId();
                    inventoryService.removeStock(inventoryId, (long) cartItem.getQuantity().intValue());
                } else {
                    log.warn("No inventory found for product {} — stock not deducted", cartItem.getProductId());
                }
            } catch (Exception e) {
                log.warn("Inventory deduction failed for product {}: {}", cartItem.getProductId(), e.getMessage());
            }

            summaries.add(CheckoutResponse.CheckoutItemSummary.builder()
                    .productName(cartItem.getProductName())
                    .sku(cartItem.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .discountValue(cartItem.getDiscountValue())
                    .lineTotal(cartItem.getLineTotal())
                    .build());
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {} (total={})", orderNumber, total);

        // --- Mark cart completed ---
        cart.setStatus(CartStatus.COMPLETED);
        cart.setCompletedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // --- Shop info for receipt ---
        ShopInfoDTO shopInfo = null;
        try {
            shopInfo = shopInfoService.getShopInfo();
        } catch (Exception e) {
            log.warn("Could not load shop info for receipt: {}", e.getMessage());
        }

        // Back-fill loyalty transaction's orderId now that we have the saved order ID
        if (loyaltyPointsRedeemed > 0 && resolvedCustomerId != null) {
            try {
                loyaltyService.backfillRedemptionOrderId(resolvedCustomerId, savedOrder.getId());
            } catch (Exception e) {
                log.warn("Failed to back-fill loyalty order ID: {}", e.getMessage());
            }
        }

        return CheckoutResponse.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .shopName(shopInfo != null ? shopInfo.getShopName() : null)
                .shopAddress(shopInfo != null ? shopInfo.getAddress() : null)
                .shopTaxId(shopInfo != null ? shopInfo.getSupplierTaxCode() : null)
                .items(summaries)
                .subtotal(itemsSubtotal)
                .itemDiscount(itemDiscount)
                .orderDiscount(orderDiscount)
                .promotionDiscount(promotionDiscount)
                .loyaltyDiscount(loyaltyDiscount)
                .totalDiscount(totalDiscount.add(loyaltyDiscount))
                .totalTax(totalTax)
                .total(total)
                .paymentMethod(paymentMethod)
                .amountPaid(amountPaid)
                .changeAmount(changeAmount)
                .customerId(resolvedCustomer != null ? resolvedCustomer.getId() : null)
                .customerName(resolvedCustomerName)
                .notes(request.getNotes())
                .promotionCode(appliedPromotionCode)
                .loyaltyPointsRedeemed(loyaltyPointsRedeemed > 0 ? loyaltyPointsRedeemed : null)
                .completedAt(savedOrder.getCompletedAt())
                .build();
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "ORD-" + datePart + "-" + seq;
    }

    /**
     * Mark cart as abandoned
     * Called when user leaves without checkout
     */
    @Override
    public void abandonCart(String cartId) {
        log.info("Abandoning cart: {}", cartId);

        CartEntity cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("cart.not.found")));

        cart.setStatus(CartStatus.ABANDONED);
        cart.setAbandonedAt(LocalDateTime.now());

        cartRepository.save(cart);
        log.info("Cart abandoned");
    }

    // ===== Helper Methods =====

    /**
     * Convert CartEntity to CartResponse DTO
     */
    private CartResponse mapToResponse(CartEntity cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .cartId(cart.getCartId())
                .customerId(cart.getCustomerId())
                .items(items)
                .subtotal(cart.getSubtotal())
                .totalDiscount(cart.getTotalDiscount())
                .totalTax(cart.getTotalTax())
                .total(cart.getTotal())
                .status(cart.getStatus())
                .appliedCoupons(parseCoupons(cart.getAppliedCoupons()))
                .appliedPromotions(parsePromotions(cart.getAppliedPromotions()))
                .notes(cart.getNotes())
                .totalItemCount(cart.getTotalItemCount())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * Convert CartItemEntity to CartItemResponse DTO
     */
    private CartItemResponse mapItemToResponse(CartItemEntity item) {
        Map<String, String> variants = parseVariants(item.getVariants());

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .barcode(item.getBarcode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .basePrice(item.getBasePrice())
                .unitCost(item.getUnitCost())
                .discountType(item.getDiscountType())
                .discountValue(item.getDiscountValue())
                .discountReason(item.getDiscountReason())
                .lineSubtotal(item.getLineSubtotal())
                .lineTotal(item.getLineTotal())
                .tax(item.getTax())
                .lineGrandTotal(item.getLineGrandTotal())
                .variants(variants)
                .notes(item.getNotes())
                .build();
    }

    /**
     * Parse JSON array of coupons
     */
    private List<String> parseCoupons(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing coupons JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse JSON array of promotions
     */
    private List<String> parsePromotions(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing promotions JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse JSON variants object
     */
    private Map<String, String> parseVariants(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new java.util.HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing variants JSON: {}", json, e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return "[]";
        }
    }
}


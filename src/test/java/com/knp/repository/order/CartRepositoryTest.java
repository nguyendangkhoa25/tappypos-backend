package com.knp.repository.order;

import com.knp.model.entity.order.CartEntity;
import com.knp.model.entity.order.CartItemEntity;
import com.knp.model.enums.CartStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Cart Repository Tests")
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Test
    @DisplayName("Should save and find cart by cartId")
    void testFindByCartId() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("test-cart-123")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        // Act
        CartEntity saved = cartRepository.save(cart);
        Optional<CartEntity> found = cartRepository.findByCartId("test-cart-123");

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("test-cart-123", found.get().getCartId());
    }

    @Test
    @DisplayName("Should return empty Optional when cart not found")
    void testFindByCartId_NotFound() {
        // Act
        Optional<CartEntity> found = cartRepository.findByCartId("non-existent");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should find cart by customer and status")
    void testFindByCustomerIdAndStatus() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("customer-cart-123")
            .customerId(42L)
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        // Act
        cartRepository.save(cart);
        Optional<CartEntity> found = cartRepository.findByCustomerIdAndStatus(42L, CartStatus.ACTIVE);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(42L, found.get().getCustomerId());
        assertEquals(CartStatus.ACTIVE, found.get().getStatus());
    }

    @Test
    @DisplayName("Should find all carts by status")
    void testFindByStatus() {
        // Arrange
        CartEntity cart1 = CartEntity.builder()
            .cartId("active-cart-1")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        CartEntity cart2 = CartEntity.builder()
            .cartId("abandoned-cart-1")
            .status(CartStatus.ABANDONED)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        // Act
        cartRepository.save(cart1);
        cartRepository.save(cart2);
        java.util.List<CartEntity> activeCarts = cartRepository.findByStatus(CartStatus.ACTIVE);

        // Assert
        assertTrue(activeCarts.size() >= 1);
        assertTrue(activeCarts.stream().allMatch(c -> c.getStatus() == CartStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should find all carts for customer")
    void testFindByCustomerId() {
        // Arrange
        CartEntity cart1 = CartEntity.builder()
            .cartId("customer-42-cart-1")
            .customerId(42L)
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        CartEntity cart2 = CartEntity.builder()
            .cartId("customer-42-cart-2")
            .customerId(42L)
            .status(CartStatus.ABANDONED)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        // Act
        cartRepository.save(cart1);
        cartRepository.save(cart2);
        java.util.List<CartEntity> customerCarts = cartRepository.findByCustomerId(42L);

        // Assert
        assertTrue(customerCarts.size() >= 2);
        assertTrue(customerCarts.stream().allMatch(c -> c.getCustomerId() == 42L));
    }

    @Test
    @DisplayName("Should count carts by status")
    void testCountByStatus() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("counted-cart")
            .status(CartStatus.PAID)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();

        // Act
        cartRepository.save(cart);
        Long count = cartRepository.countByStatus(CartStatus.PAID);

        // Assert
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("Should delete cart")
    void testDelete() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("delete-test-cart")
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

        // Act
        cartRepository.deleteById(saved.getId());
        Optional<CartEntity> found = cartRepository.findById(saved.getId());

        // Assert
        assertTrue(found.isEmpty());
    }
}

@DataJpaTest
@DisplayName("Cart Item Repository Tests")
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    @DisplayName("Should save and find cart items by cart ID")
    void testFindByCartId() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("item-test-cart")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();
        CartEntity savedCart = cartRepository.save(cart);

        CartItemEntity item = CartItemEntity.builder()
            .cart(savedCart)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .unitCost(BigDecimal.ZERO)
            .lineSubtotal(BigDecimal.TEN)
            .lineTotal(BigDecimal.TEN)
            .tax(BigDecimal.ONE)
            .lineGrandTotal(BigDecimal.valueOf(11))
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Act
        cartItemRepository.save(item);
        java.util.List<CartItemEntity> items = cartItemRepository.findByCartId(savedCart.getId());

        // Assert
        assertEquals(1, items.size());
        assertEquals("Test Product", items.get(0).getProductName());
    }

    @Test
    @DisplayName("Should find cart items by cart and product ID")
    void testFindByCartIdAndProductId() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("product-test-cart")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();
        CartEntity savedCart = cartRepository.save(cart);

        CartItemEntity item = CartItemEntity.builder()
            .cart(savedCart)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .unitCost(BigDecimal.ZERO)
            .lineSubtotal(BigDecimal.TEN)
            .lineTotal(BigDecimal.TEN)
            .tax(BigDecimal.ONE)
            .lineGrandTotal(BigDecimal.valueOf(11))
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Act
        cartItemRepository.save(item);
        java.util.List<CartItemEntity> items = cartItemRepository
            .findByCartIdAndProductId(savedCart.getId(), 1L);

        // Assert
        assertEquals(1, items.size());
        assertEquals(1L, items.get(0).getProductId());
    }

    @Test
    @DisplayName("Should return empty list when no items found")
    void testFindByCartId_EmptyList() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("empty-cart")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();
        CartEntity savedCart = cartRepository.save(cart);

        // Act
        java.util.List<CartItemEntity> items = cartItemRepository.findByCartId(savedCart.getId());

        // Assert
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("Should delete cart item")
    void testDelete() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("delete-item-cart")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();
        CartEntity savedCart = cartRepository.save(cart);

        CartItemEntity item = CartItemEntity.builder()
            .cart(savedCart)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .unitCost(BigDecimal.ZERO)
            .lineSubtotal(BigDecimal.TEN)
            .lineTotal(BigDecimal.TEN)
            .tax(BigDecimal.ONE)
            .lineGrandTotal(BigDecimal.valueOf(11))
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CartItemEntity savedItem = cartItemRepository.save(item);

        // Act
        cartItemRepository.deleteById(savedItem.getId());
        Optional<CartItemEntity> found = cartItemRepository.findById(savedItem.getId());

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should find multiple items in same cart")
    void testFindMultipleItems() {
        // Arrange
        CartEntity cart = CartEntity.builder()
            .cartId("multi-item-cart")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .build();
        CartEntity savedCart = cartRepository.save(cart);

        CartItemEntity item1 = CartItemEntity.builder()
            .cart(savedCart)
            .productId(1L)
            .productName("Product 1")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .unitPrice(BigDecimal.TEN)
            .unitCost(BigDecimal.ZERO)
            .lineSubtotal(BigDecimal.TEN)
            .lineTotal(BigDecimal.TEN)
            .tax(BigDecimal.ONE)
            .lineGrandTotal(BigDecimal.valueOf(11))
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CartItemEntity item2 = CartItemEntity.builder()
            .cart(savedCart)
            .productId(2L)
            .productName("Product 2")
            .sku("SKU-002")
            .quantity(2)
            .basePrice(BigDecimal.valueOf(20))
            .unitPrice(BigDecimal.valueOf(20))
            .unitCost(BigDecimal.ZERO)
            .lineSubtotal(BigDecimal.valueOf(40))
            .lineTotal(BigDecimal.valueOf(40))
            .tax(BigDecimal.valueOf(4))
            .lineGrandTotal(BigDecimal.valueOf(44))
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Act
        cartItemRepository.save(item1);
        cartItemRepository.save(item2);
        java.util.List<CartItemEntity> items = cartItemRepository.findByCartId(savedCart.getId());

        // Assert
        assertEquals(2, items.size());
    }
}


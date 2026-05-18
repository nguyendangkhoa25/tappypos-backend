package com.tappy.pos.model.entity.order;

import com.tappy.pos.model.enums.CartStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart Entity Tests")
class CartEntityTest {

    private CartEntity cart;

    @BeforeEach
    void setUp() {
        cart = CartEntity.builder()
            .id(1L)
            .cartId("test-cart-id")
            .status(CartStatus.ACTIVE)
            .subtotal(BigDecimal.ZERO)
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.ZERO)
            .total(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .appliedCoupons("[]")
            .appliedPromotions("[]")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ==================== Add Item Tests ====================

    @Test
    @DisplayName("Should add item to cart")
    void testAddItem() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();

        // Act
        cart.addItem(item);

        // Assert
        assertEquals(1, cart.getItems().size());
        assertEquals(cart, item.getCart());
    }

    @Test
    @DisplayName("Should add multiple items to cart")
    void testAddMultipleItems() {
        // Arrange
        CartItemEntity item1 = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Product 1")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();

        CartItemEntity item2 = CartItemEntity.builder()
            .id(2L)
            .productId(2L)
            .productName("Product 2")
            .quantity(2)
            .basePrice(BigDecimal.valueOf(20))
            .build();

        // Act
        cart.addItem(item1);
        cart.addItem(item2);

        // Assert
        assertEquals(2, cart.getItems().size());
    }

    @Test
    @DisplayName("Should not add null item")
    void testAddNullItem() {
        // Act
        cart.addItem(null);

        // Assert
        assertEquals(0, cart.getItems().size());
    }

    // ==================== Remove Item Tests ====================

    @Test
    @DisplayName("Should remove item from cart")
    void testRemoveItem() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();
        cart.addItem(item);

        // Act
        cart.removeItem(item);

        // Assert
        assertEquals(0, cart.getItems().size());
        assertNull(item.getCart());
    }

    @Test
    @DisplayName("Should not remove null item")
    void testRemoveNullItem() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();
        cart.addItem(item);

        // Act
        cart.removeItem(null);

        // Assert
        assertEquals(1, cart.getItems().size());
    }

    // ==================== Clear Items Tests ====================

    @Test
    @DisplayName("Should clear all items from cart")
    void testClearItems() {
        // Arrange
        CartItemEntity item1 = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Product 1")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();

        CartItemEntity item2 = CartItemEntity.builder()
            .id(2L)
            .productId(2L)
            .productName("Product 2")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(20))
            .build();

        cart.addItem(item1);
        cart.addItem(item2);

        // Act
        cart.clearItems();

        // Assert
        assertEquals(0, cart.getItems().size());
        assertNull(item1.getCart());
        assertNull(item2.getCart());
    }

    @Test
    @DisplayName("Should clear empty cart without error")
    void testClearEmptyCart() {
        // Act & Assert
        assertDoesNotThrow(() -> cart.clearItems());
        assertEquals(0, cart.getItems().size());
    }

    // ==================== Get Total Item Count Tests ====================

    @Test
    @DisplayName("Should return 0 for empty cart")
    void testGetTotalItemCount_EmptyCart() {
        // Act
        Integer count = cart.getTotalItemCount();

        // Assert
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Should return correct count for single item")
    void testGetTotalItemCount_SingleItem() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(3)
            .basePrice(BigDecimal.TEN)
            .build();
        cart.addItem(item);

        // Act
        Integer count = cart.getTotalItemCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should return sum of quantities for multiple items")
    void testGetTotalItemCount_MultipleItems() {
        // Arrange
        CartItemEntity item1 = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Product 1")
            .quantity(2)
            .basePrice(BigDecimal.TEN)
            .build();

        CartItemEntity item2 = CartItemEntity.builder()
            .id(2L)
            .productId(2L)
            .productName("Product 2")
            .quantity(3)
            .basePrice(BigDecimal.valueOf(20))
            .build();

        cart.addItem(item1);
        cart.addItem(item2);

        // Act
        Integer count = cart.getTotalItemCount();

        // Assert
        assertEquals(5, count); // 2 + 3
    }

    // ==================== Is Empty Tests ====================

    @Test
    @DisplayName("Should return true for empty cart")
    void testIsEmpty_True() {
        // Act
        boolean isEmpty = cart.isEmpty();

        // Assert
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Should return false for non-empty cart")
    void testIsEmpty_False() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.TEN)
            .build();
        cart.addItem(item);

        // Act
        boolean isEmpty = cart.isEmpty();

        // Assert
        assertFalse(isEmpty);
    }

    @Test
    @DisplayName("Should return true when items list is null")
    void testIsEmpty_NullItems() {
        // Arrange
        cart.setItems(null);

        // Act
        boolean isEmpty = cart.isEmpty();

        // Assert
        assertTrue(isEmpty);
    }

    // ==================== Recalculate Totals Tests ====================

    @Test
    @DisplayName("Should set totals to zero for empty cart")
    void testRecalculateTotals_EmptyCart() {
        // Act
        cart.recalculateTotals();

        // Assert
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
        assertEquals(BigDecimal.ZERO, cart.getTotalDiscount());
        assertEquals(BigDecimal.ZERO, cart.getTotalTax());
        assertEquals(BigDecimal.ZERO, cart.getTotal());
    }

    @Test
    @DisplayName("Should calculate totals for single item")
    void testRecalculateTotals_SingleItem() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .lineSubtotal(BigDecimal.valueOf(100))
            .discountValue(BigDecimal.ZERO)
            .build();
        cart.addItem(item);

        // Act
        cart.recalculateTotals();

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(cart.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalDiscount()));
        // Tax should be 10% of (100 - 0) = 10
        assertEquals(0, BigDecimal.valueOf(10).compareTo(cart.getTotalTax()));
        // Total = 100 + 10 - 0 = 110
        assertEquals(0, BigDecimal.valueOf(110).compareTo(cart.getTotal()));
    }

    @Test
    @DisplayName("Should calculate totals with multiple items")
    void testRecalculateTotals_MultipleItems() {
        // Arrange
        CartItemEntity item1 = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Product 1")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .lineSubtotal(BigDecimal.valueOf(100))
            .discountValue(BigDecimal.ZERO)
            .build();

        CartItemEntity item2 = CartItemEntity.builder()
            .id(2L)
            .productId(2L)
            .productName("Product 2")
            .quantity(2)
            .basePrice(BigDecimal.valueOf(50))
            .lineSubtotal(BigDecimal.valueOf(100))
            .discountValue(BigDecimal.ZERO)
            .build();

        cart.addItem(item1);
        cart.addItem(item2);

        // Act
        cart.recalculateTotals();

        // Assert
        assertEquals(0, BigDecimal.valueOf(200).compareTo(cart.getSubtotal())); // 100 + 100
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalDiscount()));
        // Tax = 10% of (200 - 0) = 20
        assertEquals(0, BigDecimal.valueOf(20).compareTo(cart.getTotalTax()));
        // Total = 200 + 20 - 0 = 220
        assertEquals(0, BigDecimal.valueOf(220).compareTo(cart.getTotal()));
    }

    @Test
    @DisplayName("Should calculate totals with discounts")
    void testRecalculateTotals_WithDiscounts() {
        // Arrange
        CartItemEntity item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .lineSubtotal(BigDecimal.valueOf(100))
            .discountValue(BigDecimal.valueOf(20))
            .build();
        cart.addItem(item);

        // Act
        cart.recalculateTotals();

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(cart.getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(cart.getTotalDiscount()));
        // Tax = 10% of (100 - 20) = 8
        assertEquals(0, BigDecimal.valueOf(8).compareTo(cart.getTotalTax()));
        // Total = 100 + 8 - 20 = 88
        assertEquals(0, BigDecimal.valueOf(88).compareTo(cart.getTotal()));
    }

    @Test
    @DisplayName("Should handle null items list in recalculate")
    void testRecalculateTotals_NullItems() {
        // Arrange
        cart.setItems(null);

        // Act
        cart.recalculateTotals();

        // Assert
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
        assertEquals(BigDecimal.ZERO, cart.getTotalDiscount());
        assertEquals(BigDecimal.ZERO, cart.getTotalTax());
        assertEquals(BigDecimal.ZERO, cart.getTotal());
    }

    // ==================== Cart Status Tests ====================

    @Test
    @DisplayName("Should create cart with ACTIVE status")
    void testCartStatus_Active() {
        // Assert
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }

    @Test
    @DisplayName("Should change cart status to ABANDONED")
    void testCartStatus_Abandoned() {
        // Act
        cart.setStatus(CartStatus.ABANDONED);

        // Assert
        assertEquals(CartStatus.ABANDONED, cart.getStatus());
    }

    @Test
    @DisplayName("Should change cart status to COMPLETED")
    void testCartStatus_Completed() {
        // Act
        cart.setStatus(CartStatus.COMPLETED);

        // Assert
        assertEquals(CartStatus.COMPLETED, cart.getStatus());
    }

    @Test
    @DisplayName("Should change cart status to PAID")
    void testCartStatus_Paid() {
        // Act
        cart.setStatus(CartStatus.PAID);

        // Assert
        assertEquals(CartStatus.PAID, cart.getStatus());
    }

    // ==================== Builder Tests ====================

    @Test
    @DisplayName("Should create cart with builder")
    void testBuilder() {
        // Act & Assert
        assertNotNull(cart);
        assertEquals(1L, cart.getId());
        assertEquals("test-cart-id", cart.getCartId());
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }

    @Test
    @DisplayName("Should have timestamps after creation")
    void testTimestamps() {
        // Assert
        assertNotNull(cart.getCreatedAt(), "createdAt should not be null");
        assertNotNull(cart.getUpdatedAt(), "updatedAt should not be null");
    }
}


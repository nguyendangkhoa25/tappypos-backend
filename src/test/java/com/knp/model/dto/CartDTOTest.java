package com.knp.model.dto;

import com.knp.model.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart Request DTO Tests")
class CartRequestTest {

    private CartRequest request;

    @BeforeEach
    void setUp() {
        request = CartRequest.builder()
            .cartId("test-cart")
            .productId(1L)
            .quantity(1)
            .build();
    }

    @Test
    @DisplayName("Should create cart request with builder")
    void testBuilder() {
        // Assert
        assertNotNull(request);
        assertEquals("test-cart", request.getCartId());
        assertEquals(1L, request.getProductId());
        assertEquals(1, request.getQuantity());
    }

    @Test
    @DisplayName("Should set and get all properties")
    void testProperties() {
        // Act
        request.setCustomerId(42L);
        request.setVariants(new HashMap<>(Map.of("size", "M")));
        request.setCartItemId(5L);
        request.setNewQuantity(3);
        request.setDiscountType(DiscountType.AMOUNT);
        request.setDiscountValue(BigDecimal.TEN);
        request.setDiscountReason("Test");
        request.setCouponCode("SUMMER20");
        request.setPromotionId("PROMO123");
        request.setNotes("Test notes");

        // Assert
        assertEquals(42L, request.getCustomerId());
        assertNotNull(request.getVariants());
        assertEquals(5L, request.getCartItemId());
        assertEquals(3, request.getNewQuantity());
        assertEquals(DiscountType.AMOUNT, request.getDiscountType());
        assertEquals(BigDecimal.TEN, request.getDiscountValue());
        assertEquals("Test", request.getDiscountReason());
        assertEquals("SUMMER20", request.getCouponCode());
        assertEquals("PROMO123", request.getPromotionId());
        assertEquals("Test notes", request.getNotes());
    }

    @Test
    @DisplayName("Should support variants in request")
    void testVariants() {
        // Arrange
        Map<String, String> variants = new HashMap<>();
        variants.put("size", "M");
        variants.put("color", "Red");

        // Act
        request.setVariants(variants);

        // Assert
        assertEquals(2, request.getVariants().size());
        assertEquals("M", request.getVariants().get("size"));
        assertEquals("Red", request.getVariants().get("color"));
    }

    @Test
    @DisplayName("Should allow null variants")
    void testNullVariants() {
        // Act
        request.setVariants(null);

        // Assert
        assertNull(request.getVariants());
    }
}

@DisplayName("Cart Response DTO Tests")
class CartResponseTest {

    private CartResponse response;

    @BeforeEach
    void setUp() {
        response = CartResponse.builder()
            .id(1L)
            .cartId("test-cart")
            .subtotal(BigDecimal.valueOf(100))
            .totalDiscount(BigDecimal.ZERO)
            .totalTax(BigDecimal.valueOf(10))
            .total(BigDecimal.valueOf(110))
            .items(new ArrayList<>())
            .appliedCoupons(new ArrayList<>())
            .appliedPromotions(new ArrayList<>())
            .build();
    }

    @Test
    @DisplayName("Should create cart response with builder")
    void testBuilder() {
        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test-cart", response.getCartId());
        assertEquals(BigDecimal.valueOf(100), response.getSubtotal());
        assertEquals(BigDecimal.valueOf(110), response.getTotal());
    }

    @Test
    @DisplayName("Should set and get all properties")
    void testProperties() {
        // Act
        response.setCustomerId(42L);
        response.setTotalItemCount(5);
        response.setNotes("Test notes");

        // Assert
        assertEquals(42L, response.getCustomerId());
        assertEquals(5, response.getTotalItemCount());
        assertEquals("Test notes", response.getNotes());
    }

    @Test
    @DisplayName("Should support items in response")
    void testItems() {
        // Arrange
        CartItemResponse item = CartItemResponse.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .quantity(1)
            .build();

        // Act
        response.getItems().add(item);

        // Assert
        assertEquals(1, response.getItems().size());
        assertEquals("Test Product", response.getItems().get(0).getProductName());
    }

    @Test
    @DisplayName("Should support coupons in response")
    void testCoupons() {
        // Act
        response.getAppliedCoupons().add("SUMMER20");
        response.getAppliedCoupons().add("FALL10");

        // Assert
        assertEquals(2, response.getAppliedCoupons().size());
        assertTrue(response.getAppliedCoupons().contains("SUMMER20"));
    }
}

@DisplayName("Cart Item Response DTO Tests")
class CartItemResponseTest {

    private CartItemResponse response;

    @BeforeEach
    void setUp() {
        response = CartItemResponse.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .unitPrice(BigDecimal.valueOf(100))
            .lineSubtotal(BigDecimal.valueOf(100))
            .lineTotal(BigDecimal.valueOf(100))
            .tax(BigDecimal.valueOf(10))
            .lineGrandTotal(BigDecimal.valueOf(110))
            .build();
    }

    @Test
    @DisplayName("Should create cart item response with builder")
    void testBuilder() {
        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getProductName());
        assertEquals("SKU-001", response.getSku());
        assertEquals(1, response.getQuantity());
    }

    @Test
    @DisplayName("Should set and get all properties")
    void testProperties() {
        // Act
        response.setBarcode("BARCODE-001");
        response.setDiscountType(DiscountType.AMOUNT);
        response.setDiscountValue(BigDecimal.TEN);
        response.setDiscountReason("Test");
        response.setNotes("Test notes");

        // Assert
        assertEquals("BARCODE-001", response.getBarcode());
        assertEquals(DiscountType.AMOUNT, response.getDiscountType());
        assertEquals(BigDecimal.TEN, response.getDiscountValue());
        assertEquals("Test", response.getDiscountReason());
        assertEquals("Test notes", response.getNotes());
    }

    @Test
    @DisplayName("Should support variants in response")
    void testVariants() {
        // Arrange
        Map<String, String> variants = new HashMap<>(Map.of("size", "M", "color", "Red"));

        // Act
        response.setVariants(variants);

        // Assert
        assertEquals(2, response.getVariants().size());
        assertEquals("M", response.getVariants().get("size"));
    }
}


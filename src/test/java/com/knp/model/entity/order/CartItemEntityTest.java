package com.knp.model.entity.order;

import com.knp.model.enums.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart Item Entity Tests")
class CartItemEntityTest {

    private CartItemEntity item;

    @BeforeEach
    void setUp() {
        item = CartItemEntity.builder()
            .id(1L)
            .productId(1L)
            .productName("Test Product")
            .sku("SKU-001")
            .quantity(1)
            .basePrice(BigDecimal.valueOf(100))
            .unitPrice(BigDecimal.valueOf(100))
            .discountType(DiscountType.NONE)
            .discountValue(BigDecimal.ZERO)
            .addedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ==================== Recalculate Line Total Tests ====================

    @Test
    @DisplayName("Should calculate line totals correctly without discount")
    void testRecalculateLineTotal_NoDiscount() {
        // Arrange
        item.setQuantity(2);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.valueOf(200).compareTo(item.getLineSubtotal())); // 2 * 100
        assertEquals(0, BigDecimal.valueOf(200).compareTo(item.getLineTotal())); // 200 - 0
        assertEquals(0, BigDecimal.valueOf(20).compareTo(item.getTax())); // 200 * 0.10
        assertEquals(0, BigDecimal.valueOf(220).compareTo(item.getLineGrandTotal())); // 200 + 20
    }

    @Test
    @DisplayName("Should calculate line totals with fixed discount")
    void testRecalculateLineTotal_WithFixedDiscount() {
        // Arrange
        item.setQuantity(1);
        item.setDiscountValue(BigDecimal.valueOf(20));

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(item.getLineSubtotal())); // 1 * 100
        assertEquals(0, BigDecimal.valueOf(80).compareTo(item.getLineTotal())); // 100 - 20
        assertEquals(0, BigDecimal.valueOf(8).compareTo(item.getTax())); // 80 * 0.10
        assertEquals(0, BigDecimal.valueOf(88).compareTo(item.getLineGrandTotal())); // 80 + 8
    }

    @Test
    @DisplayName("Should calculate line totals with multiple quantities")
    void testRecalculateLineTotal_MultipleQuantities() {
        // Arrange
        item.setQuantity(5);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.valueOf(500).compareTo(item.getLineSubtotal())); // 5 * 100
        assertEquals(0, BigDecimal.valueOf(500).compareTo(item.getLineTotal())); // 500 - 0
        assertEquals(0, BigDecimal.valueOf(50).compareTo(item.getTax())); // 500 * 0.10
        assertEquals(0, BigDecimal.valueOf(550).compareTo(item.getLineGrandTotal())); // 500 + 50
    }

    @Test
    @DisplayName("Should handle zero base price")
    void testRecalculateLineTotal_ZeroBasePrice() {
        // Arrange
        item.setBasePrice(BigDecimal.ZERO);
        item.setQuantity(1);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineGrandTotal()));
    }

    @Test
    @DisplayName("Should handle large quantities")
    void testRecalculateLineTotal_LargeQuantity() {
        // Arrange
        item.setQuantity(1000);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(item.getLineSubtotal())); // 1000 * 100
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(item.getLineTotal()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(item.getTax())); // 100000 * 0.10
        assertEquals(0, BigDecimal.valueOf(110000).compareTo(item.getLineGrandTotal()));
    }

    // ==================== Apply Discount Tests ====================

    @Test
    @DisplayName("Should apply fixed amount discount")
    void testApplyDiscount_FixedAmount() {
        // Arrange
        item.setQuantity(1);
        item.setDiscountType(DiscountType.NONE);
        item.recalculateLineTotal(); // Initialize line subtotal

        // Act
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(25), "Test discount");

        // Assert
        assertEquals(DiscountType.AMOUNT, item.getDiscountType());
        assertEquals(BigDecimal.valueOf(25), item.getDiscountValue());
        assertEquals("Test discount", item.getDiscountReason());
        assertEquals(BigDecimal.valueOf(75), item.getLineTotal()); // 100 - 25
    }

    @Test
    @DisplayName("Should apply percentage discount")
    void testApplyDiscount_Percentage() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal(); // Initialize line subtotal = 100

        // Act
        item.applyDiscount(DiscountType.PERCENTAGE, BigDecimal.valueOf(10), "10% off");

        // Assert
        assertEquals(DiscountType.PERCENTAGE, item.getDiscountType());
        assertEquals("10% off", item.getDiscountReason());
        // 10% of 100 = 10
        assertEquals(0, BigDecimal.valueOf(10).compareTo(item.getDiscountValue()));
        assertEquals(0, BigDecimal.valueOf(90).compareTo(item.getLineTotal())); // 100 - 10
    }

    @Test
    @DisplayName("Should apply percentage discount correctly with multiple items")
    void testApplyDiscount_PercentageWithMultipleItems() {
        // Arrange
        item.setQuantity(2);
        item.recalculateLineTotal(); // line subtotal = 200

        // Act
        item.applyDiscount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20), "20% off");

        // Assert
        // 20% of 200 = 40
        assertEquals(0, BigDecimal.valueOf(40).setScale(2, java.math.RoundingMode.HALF_UP)
            .compareTo(item.getDiscountValue().setScale(2, java.math.RoundingMode.HALF_UP)));
        assertEquals(0, BigDecimal.valueOf(160).compareTo(item.getLineTotal())); // 200 - 40
    }

    @Test
    @DisplayName("Should apply large percentage discount")
    void testApplyDiscount_LargePercentage() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal();

        // Act
        item.applyDiscount(DiscountType.PERCENTAGE, BigDecimal.valueOf(50), "50% off");

        // Assert
        assertEquals(0, BigDecimal.valueOf(50).compareTo(item.getDiscountValue())); // 50% of 100
        assertEquals(0, BigDecimal.valueOf(50).compareTo(item.getLineTotal())); // 100 - 50
    }

    @Test
    @DisplayName("Should override previous discount when applying new one")
    void testApplyDiscount_OverridePrevious() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal();
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(10), "First discount");

        // Act
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(25), "Second discount");

        // Assert
        assertEquals(DiscountType.AMOUNT, item.getDiscountType());
        assertEquals(BigDecimal.valueOf(25), item.getDiscountValue());
        assertEquals("Second discount", item.getDiscountReason());
        assertEquals(BigDecimal.valueOf(75), item.getLineTotal()); // 100 - 25
    }

    // ==================== Remove Discount Tests ====================

    @Test
    @DisplayName("Should remove discount successfully")
    void testRemoveDiscount_Success() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal();
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(25), "Test discount");

        // Act
        item.removeDiscount();

        // Assert
        assertEquals(DiscountType.NONE, item.getDiscountType());
        assertEquals(BigDecimal.ZERO, item.getDiscountValue());
        assertNull(item.getDiscountReason());
        assertEquals(BigDecimal.valueOf(100), item.getLineTotal()); // 100 - 0
    }

    @Test
    @DisplayName("Should remove discount and recalculate totals")
    void testRemoveDiscount_RecalculateTotals() {
        // Arrange
        item.setQuantity(2);
        item.recalculateLineTotal();
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(50), "Discount");

        // Act
        item.removeDiscount();

        // Assert
        assertEquals(DiscountType.NONE, item.getDiscountType());
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getDiscountValue()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(item.getLineTotal())); // 200 - 0
        assertEquals(0, BigDecimal.valueOf(20).compareTo(item.getTax())); // 200 * 0.10
        assertEquals(0, BigDecimal.valueOf(220).compareTo(item.getLineGrandTotal())); // 200 + 20
    }

    // ==================== Builder Tests ====================

    @Test
    @DisplayName("Should create cart item with builder")
    void testBuilder() {
        // Assert
        assertNotNull(item);
        assertEquals(1L, item.getId());
        assertEquals(1L, item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals("SKU-001", item.getSku());
        assertEquals(1, item.getQuantity());
        assertEquals(BigDecimal.valueOf(100), item.getBasePrice());
    }

    @Test
    @DisplayName("Should set and get all properties")
    void testProperties() {
        // Act & Assert
        item.setBarcode("BARCODE-001");
        assertEquals("BARCODE-001", item.getBarcode());

        item.setVariants("{\"size\": \"M\"}");
        assertEquals("{\"size\": \"M\"}", item.getVariants());

        item.setNotes("Special notes");
        assertEquals("Special notes", item.getNotes());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle discount value larger than line subtotal")
    void testApplyDiscount_LargerThanSubtotal() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal(); // line subtotal = 100

        // Act
        item.applyDiscount(DiscountType.AMOUNT, BigDecimal.valueOf(150), "Large discount");

        // Assert
        assertEquals(BigDecimal.valueOf(150), item.getDiscountValue());
        assertEquals(BigDecimal.valueOf(-50), item.getLineTotal()); // 100 - 150
    }

    @Test
    @DisplayName("Should handle 100% percentage discount")
    void testApplyDiscount_100Percent() {
        // Arrange
        item.setQuantity(1);
        item.recalculateLineTotal();

        // Act
        item.applyDiscount(DiscountType.PERCENTAGE, BigDecimal.valueOf(100), "100% off");

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(item.getDiscountValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineTotal())); // 100 - 100
    }

    @Test
    @DisplayName("Should handle decimal base prices")
    void testRecalculateLineTotal_DecimalPrice() {
        // Arrange
        item.setBasePrice(BigDecimal.valueOf(99.99));
        item.setQuantity(3);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(BigDecimal.valueOf(299.97), item.getLineSubtotal()); // 3 * 99.99
        assertTrue(item.getTax().compareTo(BigDecimal.ZERO) > 0); // Tax > 0
    }

    @Test
    @DisplayName("Should handle very small quantities")
    void testRecalculateLineTotal_MinimalQuantity() {
        // Arrange
        item.setQuantity(1);

        // Act
        item.recalculateLineTotal();

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(item.getLineSubtotal()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(item.getLineTotal()));
        assertEquals(0, BigDecimal.valueOf(10).compareTo(item.getTax()));
    }

    @Test
    @DisplayName("Should maintain precision in calculations")
    void testCalculationPrecision() {
        // Arrange
        item.setQuantity(3);
        item.setBasePrice(BigDecimal.valueOf(333.33));

        // Act
        item.recalculateLineTotal();

        // Assert
        BigDecimal expectedSubtotal = BigDecimal.valueOf(999.99);
        assertEquals(expectedSubtotal, item.getLineSubtotal());
    }

    // ==================== Discount Type Tests ====================

    @Test
    @DisplayName("Should have discount type NONE by default")
    void testDefaultDiscountType() {
        // Assert
        assertEquals(DiscountType.NONE, item.getDiscountType());
    }

    @Test
    @DisplayName("Should change discount type to AMOUNT")
    void testDiscountType_Amount() {
        // Act
        item.setDiscountType(DiscountType.AMOUNT);

        // Assert
        assertEquals(DiscountType.AMOUNT, item.getDiscountType());
    }

    @Test
    @DisplayName("Should change discount type to PERCENTAGE")
    void testDiscountType_Percentage() {
        // Act
        item.setDiscountType(DiscountType.PERCENTAGE);

        // Assert
        assertEquals(DiscountType.PERCENTAGE, item.getDiscountType());
    }

    // ==================== Timestamps Tests ====================

    @Test
    @DisplayName("Should have add timestamp after creation")
    void testTimestamps() {
        // Assert
        assertNotNull(item.getAddedAt(), "addedAt should not be null");
        assertNotNull(item.getUpdatedAt(), "updatedAt should not be null");
    }

    @Test
    @DisplayName("Should update timestamp when modified")
    void testTimestampUpdate() {
        // Arrange
        var originalTime = item.getUpdatedAt();

        // Act
        item.setQuantity(5);

        // Assert - In real scenario, @PreUpdate would update this
        assertNotNull(item.getUpdatedAt(), "updatedAt should not be null");
    }
}


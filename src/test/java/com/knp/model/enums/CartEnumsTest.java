package com.knp.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart Status Enum Tests")
class CartStatusTest {

    @Test
    @DisplayName("Should have ACTIVE status")
    void testActiveStatus() {
        assertEquals("ACTIVE", CartStatus.ACTIVE.toString());
    }

    @Test
    @DisplayName("Should have ABANDONED status")
    void testAbandonedStatus() {
        assertEquals("ABANDONED", CartStatus.ABANDONED.toString());
    }

    @Test
    @DisplayName("Should have COMPLETED status")
    void testCompletedStatus() {
        assertEquals("COMPLETED", CartStatus.COMPLETED.toString());
    }

    @Test
    @DisplayName("Should have PAID status")
    void testPaidStatus() {
        assertEquals("PAID", CartStatus.PAID.toString());
    }

    @Test
    @DisplayName("Should be able to convert from string")
    void testValueOf() {
        assertEquals(CartStatus.ACTIVE, CartStatus.valueOf("ACTIVE"));
        assertEquals(CartStatus.ABANDONED, CartStatus.valueOf("ABANDONED"));
        assertEquals(CartStatus.COMPLETED, CartStatus.valueOf("COMPLETED"));
        assertEquals(CartStatus.PAID, CartStatus.valueOf("PAID"));
    }

    @Test
    @DisplayName("Should throw exception for invalid status")
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> CartStatus.valueOf("INVALID"));
    }

    @Test
    @DisplayName("Should have all expected values")
    void testAllValues() {
        CartStatus[] values = CartStatus.values();
        assertEquals(4, values.length);
    }

    @Test
    @DisplayName("Should be comparable")
    void testComparable() {
        CartStatus status1 = CartStatus.ACTIVE;
        CartStatus status2 = CartStatus.ACTIVE;
        CartStatus status3 = CartStatus.PAID;

        assertEquals(status1, status2);
        assertNotEquals(status1, status3);
    }
}

@DisplayName("Discount Type Enum Tests")
class DiscountTypeTest {

    @Test
    @DisplayName("Should have NONE discount type")
    void testNoneType() {
        assertEquals("NONE", DiscountType.NONE.toString());
    }

    @Test
    @DisplayName("Should have AMOUNT discount type")
    void testAmountType() {
        assertEquals("AMOUNT", DiscountType.AMOUNT.toString());
    }

    @Test
    @DisplayName("Should have PERCENTAGE discount type")
    void testPercentageType() {
        assertEquals("PERCENTAGE", DiscountType.PERCENTAGE.toString());
    }

    @Test
    @DisplayName("Should be able to convert from string")
    void testValueOf() {
        assertEquals(DiscountType.NONE, DiscountType.valueOf("NONE"));
        assertEquals(DiscountType.AMOUNT, DiscountType.valueOf("AMOUNT"));
        assertEquals(DiscountType.PERCENTAGE, DiscountType.valueOf("PERCENTAGE"));
    }

    @Test
    @DisplayName("Should throw exception for invalid discount type")
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DiscountType.valueOf("INVALID"));
    }

    @Test
    @DisplayName("Should have all expected values")
    void testAllValues() {
        DiscountType[] values = DiscountType.values();
        assertEquals(3, values.length);
    }

    @Test
    @DisplayName("Should be comparable")
    void testComparable() {
        DiscountType type1 = DiscountType.AMOUNT;
        DiscountType type2 = DiscountType.AMOUNT;
        DiscountType type3 = DiscountType.PERCENTAGE;

        assertEquals(type1, type2);
        assertNotEquals(type1, type3);
    }

    @Test
    @DisplayName("Should support switch statement")
    void testSwitch() {
        DiscountType type = DiscountType.AMOUNT;
        String result = switch (type) {
            case NONE -> "No discount";
            case AMOUNT -> "Fixed amount";
            case PERCENTAGE -> "Percentage off";
        };
        assertEquals("Fixed amount", result);
    }
}


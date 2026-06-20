package com.tappy.pos.model.entity.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the base-unit conversion used by stock deduction when a line is sold
 * in the alternate unit (bán sỉ): baseQuantity() = round(quantity × unitFactor).
 */
@DisplayName("Sell-unit → base-unit conversion (OrderItem/CartItemEntity.baseQuantity)")
class SellUnitConversionTest {

    @Test
    @DisplayName("Null factor → quantity is already in the base unit (normal line)")
    void nullFactorIsIdentity() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(7);
        assertThat(oi.baseQuantity()).isEqualTo(7L);

        CartItemEntity ci = new CartItemEntity();
        ci.setQuantity(7);
        assertThat(ci.baseQuantity()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Integer factor: 2 bao × 50 kg/bao → deduct 100")
    void integerFactor() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(2);
        oi.setUnitFactor(new BigDecimal("50"));
        assertThat(oi.baseQuantity()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Fractional factor rounds half-up: 3 cây × 11.85 kg → 36 (35.55 → 36)")
    void fractionalFactorRounds() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(3);
        oi.setUnitFactor(new BigDecimal("11.850"));
        assertThat(oi.baseQuantity()).isEqualTo(36L); // 35.55 → 36
    }

    @Test
    @DisplayName("Zero/negative factor falls back to the raw quantity (defensive)")
    void nonPositiveFactorIsIdentity() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(4);
        oi.setUnitFactor(BigDecimal.ZERO);
        assertThat(oi.baseQuantity()).isEqualTo(4L);
    }
}

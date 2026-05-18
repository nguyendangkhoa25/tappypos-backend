package com.tappy.pos.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryConstant Tests")
class InventoryConstantTest {

    @Test
    @DisplayName("All endpoint constants are non-null strings")
    void constants_areNonNull() {
        assertThat(InventoryConstant.INVENTORY_API_ENDPOINT).isNotNull();
        assertThat(InventoryConstant.INVENTORY_GET_BY_ID).contains(InventoryConstant.INVENTORY_API_ENDPOINT);
        assertThat(InventoryConstant.INVENTORY_GET_BY_PRODUCT).contains(InventoryConstant.INVENTORY_API_ENDPOINT);
        assertThat(InventoryConstant.INVENTORY_ALERTS_LOW_STOCK).isNotNull();
        assertThat(InventoryConstant.INVENTORY_ALERTS_EXPIRED).isNotNull();
        assertThat(InventoryConstant.INVENTORY_ALERTS_EXPIRING_SOON).isNotNull();
        assertThat(InventoryConstant.INVENTORY_SEARCH).isNotNull();
        assertThat(InventoryConstant.INVENTORY_WAREHOUSE).isNotNull();
        assertThat(InventoryConstant.INVENTORY_TYPE).isNotNull();
        assertThat(InventoryConstant.INVENTORY_VALUE_TOTAL).isNotNull();
        assertThat(InventoryConstant.INVENTORY_UPDATE_QUANTITY).isNotNull();
        assertThat(InventoryConstant.INVENTORY_ADD_STOCK).isNotNull();
        assertThat(InventoryConstant.INVENTORY_REMOVE_STOCK).isNotNull();
    }

    @Test
    @DisplayName("Status and type constants have expected values")
    void statusAndTypeConstants() {
        assertThat(InventoryConstant.INVENTORY_STATUS_ACTIVE).isEqualTo("ACTIVE");
        assertThat(InventoryConstant.INVENTORY_STATUS_INACTIVE).isEqualTo("INACTIVE");
        assertThat(InventoryConstant.INVENTORY_STATUS_DISCONTINUED).isEqualTo("DISCONTINUED");
        assertThat(InventoryConstant.INVENTORY_TYPE_RETAIL).isEqualTo("RETAIL");
        assertThat(InventoryConstant.INVENTORY_TYPE_WHOLESALE).isEqualTo("WHOLESALE");
        assertThat(InventoryConstant.INVENTORY_TYPE_WAREHOUSE).isEqualTo("WAREHOUSE");
    }

    @Test
    @DisplayName("Default values are positive")
    void defaultValues_arePositive() {
        assertThat(InventoryConstant.DEFAULT_REORDER_LEVEL).isPositive();
        assertThat(InventoryConstant.DEFAULT_REORDER_QUANTITY).isPositive();
        assertThat(InventoryConstant.EXPIRING_SOON_DAYS).isPositive();
        assertThat(InventoryConstant.DEFAULT_PAGE_SIZE).isPositive();
        assertThat(InventoryConstant.DEFAULT_PAGE_NUMBER).isGreaterThanOrEqualTo(0);
    }
}

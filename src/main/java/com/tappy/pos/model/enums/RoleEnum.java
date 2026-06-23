package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * Role enumeration for predefined system roles
 * No new roles can be created via API - only these predefined roles are available
 */
@Getter
public enum RoleEnum {
    MASTER_TENANT("MASTER_TENANT", "Master Tenant - Full access to tenant management"),
    AGENT("AGENT", "Agent - Manages assigned shops on behalf of an agent"),
    SHOP_OWNER("SHOP_OWNER", "Shop Owner - Full access to all features"),
    CASHIER("CASHIER", "Cashier - Handles POS sales, customer transactions, front desk and pawn"),
    ACCOUNTANT("ACCOUNTANT", "Accountant - Manages revenue, salary, and invoices"),
    WAREHOUSE_STAFF("WAREHOUSE_STAFF", "Warehouse Staff - Manages inventory and stock"),
    SERVICE_STAFF("SERVICE_STAFF", "Service Staff - Takes orders and serves customers (restaurant, café, barber)"),
    TECHNICIAN("TECHNICIAN", "Technician - Skilled worker for repairs and product handling");

    private final String code;
    private final String description;

    RoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get role by code
     */
    public static void fromCode(String code) {
        for (RoleEnum role : RoleEnum.values()) {
            if (role.code.equals(code)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }

    /**
     * Check if code is valid role
     */
    public static boolean isValidRole(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}


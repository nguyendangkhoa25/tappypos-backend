package com.knp.model.enums;

import lombok.Getter;

/**
 * Role enumeration for predefined system roles
 * No new roles can be created via API - only these predefined roles are available
 */
@Getter
public enum RoleEnum {
    MASTER_TENANT("MASTER_TENANT", "Master Tenant - Full access to tenant management"),
    SHOP_OWNER("SHOP_OWNER", "Shop Owner - Full access to all features"),
    MANAGER("MANAGER", "Manager - Can manage shop, employees, and reports"),
    RECEPTIONIST("RECEPTIONIST", "Receptionist - Can manage appointments and customers"),
    CLEANER("CLEANER", "Cleaner - Can manage cleaning tasks and inventory"),
    TECHNICIAN("TECHNICIAN", "Technician/Employee - Can view appointments and customer info");

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


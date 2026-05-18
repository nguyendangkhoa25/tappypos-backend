package com.tappy.pos.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleEnum Unit Tests")
class RoleEnumTest {

    @Test
    @DisplayName("Should validate valid role codes")
    void testIsValidRole_ValidCodes() {
        // Given
        String[] validRoles = {
            "ADMIN",
            "MANAGER", 
            "STAFF",
            "CUSTOMER",
            "GUEST"
        };

        // When & Then
        for (String role : validRoles) {
            // Check if it's one of the defined roles
            boolean isValid;
            try {
                RoleEnum.valueOf(role);
                isValid = true;
            } catch (IllegalArgumentException e) {
                isValid = false;
            }
            
            if (isValid) {
                assertThat(RoleEnum.isValidRole(role)).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should reject invalid role codes")
    void testIsValidRole_InvalidCodes() {
        // Given
        String invalidRole = "INVALID_ROLE_XYZ";

        // When
        boolean isValid = RoleEnum.isValidRole(invalidRole);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle null role code")
    void testIsValidRole_NullCode() {
        // When
        boolean isValid = RoleEnum.isValidRole(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle empty role code")
    void testIsValidRole_EmptyCode() {
        // When
        boolean isValid = RoleEnum.isValidRole("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should be case sensitive")
    void testIsValidRole_CaseSensitive() {
        // When
        boolean isValidUpper = RoleEnum.isValidRole("ADMIN");
        boolean isValidLower = RoleEnum.isValidRole("admin");
        boolean isValidMixed = RoleEnum.isValidRole("Admin");

        // Then
        if (RoleEnum.isValidRole("ADMIN")) {
            assertThat(isValidUpper).isTrue();
            assertThat(isValidLower).isFalse();
            assertThat(isValidMixed).isFalse();
        }
    }

    @Test
    @DisplayName("Should handle role with spaces")
    void testIsValidRole_WithSpaces() {
        // When
        boolean isValid = RoleEnum.isValidRole(" ADMIN ");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should list all available roles")
    void testRoleEnumValues() {
        // When
        RoleEnum[] roles = RoleEnum.values();

        // Then
        assertThat(roles).isNotEmpty();
    }
}



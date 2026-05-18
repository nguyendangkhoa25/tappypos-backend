package com.tappy.pos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthContext Unit Tests")
class AuthContextTest {

    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        authContext = new AuthContext();
        // Clear any leftover state from previous tests
        authContext.clear();
    }

    @Test
    @DisplayName("Should set and get current username")
    void testSetAndGetCurrentUsername() {
        // Given
        String username = "testuser";

        // When
        authContext.setCurrentUsername(username);
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isEqualTo(username);
    }

    @Test
    @DisplayName("Should return null when no username is set")
    void testGetCurrentUsername_NoUsername() {
        // Given
        // No username set

        // When
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should clear username")
    void testClearUsername() {
        // Given
        String username = "testuser";
        authContext.setCurrentUsername(username);

        // When
        authContext.clear();
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle multiple username changes")
    void testMultipleUsernameChanges() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";

        // When & Then
        authContext.setCurrentUsername(user1);
        assertThat(authContext.getCurrentUsername()).isEqualTo(user1);

        authContext.setCurrentUsername(user2);
        assertThat(authContext.getCurrentUsername()).isEqualTo(user2);

        authContext.setCurrentUsername(user3);
        assertThat(authContext.getCurrentUsername()).isEqualTo(user3);
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testUsernameWithSpecialCharacters() {
        // Given
        String username = "test.user+tag@example.com";

        // When
        authContext.setCurrentUsername(username);
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isEqualTo(username);
    }

    @Test
    @DisplayName("Should handle empty string username")
    void testEmptyStringUsername() {
        // Given
        String emptyUsername = "";

        // When
        authContext.setCurrentUsername(emptyUsername);
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isEqualTo(emptyUsername);
    }

    @Test
    @DisplayName("Should handle long username")
    void testLongUsername() {
        // Given
        String longUsername = "a".repeat(500);

        // When
        authContext.setCurrentUsername(longUsername);
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("Should handle unicode characters in username")
    void testUnicodeUsername() {
        // Given
        String unicodeUsername = "nguyễn.đặng.khoa";

        // When
        authContext.setCurrentUsername(unicodeUsername);
        String result = authContext.getCurrentUsername();

        // Then
        assertThat(result).isEqualTo(unicodeUsername);
    }
}


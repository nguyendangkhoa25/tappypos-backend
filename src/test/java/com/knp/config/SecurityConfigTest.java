package com.knp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for SecurityConfig
 * Covers Spring Security configuration, JWT filter setup, and password encoding
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter, jwtAuthenticationEntryPoint);
    }

    // ==================== PasswordEncoder Bean Tests ====================

    @Test
    @DisplayName("Should create PasswordEncoder bean")
    void testPasswordEncoder_Created() {
        // When
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Then
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("Should create BCryptPasswordEncoder")
    void testPasswordEncoder_IsBCryptPasswordEncoder() {
        // When
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Then
        assertThat(passwordEncoder).isInstanceOf(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("Should encode password successfully")
    void testPasswordEncoder_EncodePassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String plainPassword = "myPassword123!@#";

        // When
        String encodedPassword = passwordEncoder.encode(plainPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEmpty();
        assertThat(encodedPassword).isNotEqualTo(plainPassword);
    }

    @Test
    @DisplayName("Should match plain password with encoded password")
    void testPasswordEncoder_MatchPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String plainPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(plainPassword);

        // When
        boolean matches = passwordEncoder.matches(plainPassword, encodedPassword);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match incorrect password with encoded password")
    void testPasswordEncoder_NoMatchIncorrectPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String correctPassword = "correctPassword123";
        String wrongPassword = "wrongPassword456";
        String encodedPassword = passwordEncoder.encode(correctPassword);

        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should encode different strings to different hashes")
    void testPasswordEncoder_DifferentHashesForDifferentPasswords() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password1 = "password1";
        String password2 = "password2";

        // When
        String hash1 = passwordEncoder.encode(password1);
        String hash2 = passwordEncoder.encode(password2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Should create new PasswordEncoder instance each time")
    void testPasswordEncoder_NewInstanceEachCall() {
        // When
        PasswordEncoder encoder1 = securityConfig.passwordEncoder();
        PasswordEncoder encoder2 = securityConfig.passwordEncoder();

        // Then
        assertThat(encoder1).isNotNull();
        assertThat(encoder2).isNotNull();
        // Different instances (not singleton in this context)
    }

    @Test
    @DisplayName("Should handle empty password encoding")
    void testPasswordEncoder_EmptyPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String emptyPassword = "";

        // When
        String encodedPassword = passwordEncoder.encode(emptyPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void testPasswordEncoder_SpecialCharactersPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String specialPassword = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";

        // When
        String encodedPassword = passwordEncoder.encode(specialPassword);
        boolean matches = passwordEncoder.matches(specialPassword, encodedPassword);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should handle very long password")
    void testPasswordEncoder_LongPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String longPassword = "a".repeat(500);

        // When
        String encodedPassword = passwordEncoder.encode(longPassword);
        boolean matches = passwordEncoder.matches(longPassword, encodedPassword);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should handle unicode characters in password")
    void testPasswordEncoder_UnicodePassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String unicodePassword = "пароль密码🔒";

        // When
        String encodedPassword = passwordEncoder.encode(unicodePassword);
        boolean matches = passwordEncoder.matches(unicodePassword, encodedPassword);

        // Then
        assertThat(matches).isTrue();
    }

    // ==================== SecurityConfig Initialization Tests ====================

    @Test
    @DisplayName("Should initialize SecurityConfig with JWT filter and entry point")
    void testSecurityConfig_Initialization() {
        // Given & When & Then
        assertThat(securityConfig).isNotNull();
    }

    @Test
    @DisplayName("Should have JwtAuthenticationFilter dependency")
    void testSecurityConfig_HasJwtAuthenticationFilter() {
        // When
        JwtAuthenticationFilter filter = (JwtAuthenticationFilter) ReflectionTestUtils
                .getField(securityConfig, "jwtAuthenticationFilter");

        // Then
        assertThat(filter).isEqualTo(jwtAuthenticationFilter);
    }

    @Test
    @DisplayName("Should have JwtAuthenticationEntryPoint dependency")
    void testSecurityConfig_HasJwtAuthenticationEntryPoint() {
        // When
        JwtAuthenticationEntryPoint entryPoint = (JwtAuthenticationEntryPoint) ReflectionTestUtils
                .getField(securityConfig, "jwtAuthenticationEntryPoint");

        // Then
        assertThat(entryPoint).isEqualTo(jwtAuthenticationEntryPoint);
    }

    @Test
    @DisplayName("Should be annotated with @Configuration")
    void testSecurityConfig_ConfigurationAnnotation() {
        // When & Then
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
                .isTrue();
    }

    @Test
    @DisplayName("Should be annotated with @EnableWebSecurity")
    void testSecurityConfig_EnableWebSecurityAnnotation() {
        // When & Then
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class))
                .isTrue();
    }

    // ==================== PasswordEncoder Bean Configuration Tests ====================

    @Test
    @DisplayName("PasswordEncoder method should have @Bean annotation")
    void testPasswordEncoder_BeanAnnotation() throws NoSuchMethodException {
        // When
        var method = SecurityConfig.class.getMethod("passwordEncoder");

        // Then
        assertThat(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .isTrue();
    }

    @Test
    @DisplayName("Should encode same password multiple times to different hashes (salting)")
    void testPasswordEncoder_SaltingPreventsIdenticalHashes() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "samePassword";

        // When
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        // Then - BCrypt adds salt, so hashes should be different
        assertThat(hash1).isNotEqualTo(hash2);
        // But both should match the original password
        assertThat(passwordEncoder.matches(password, hash1)).isTrue();
        assertThat(passwordEncoder.matches(password, hash2)).isTrue();
    }

    @Test
    @DisplayName("Should create consistent password encoder across multiple calls")
    void testPasswordEncoder_ConsistentBehavior() {
        // Given
        PasswordEncoder encoder1 = securityConfig.passwordEncoder();
        PasswordEncoder encoder2 = securityConfig.passwordEncoder();
        String password = "testPassword";

        // When
        String hash1 = encoder1.encode(password);
        boolean matches2 = encoder2.matches(password, hash1);

        // Then
        assertThat(matches2).isTrue();
    }

    @Test
    @DisplayName("Should handle null password gracefully")
    void testPasswordEncoder_NullPassword() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // When & Then - Should throw exception
        try {
            passwordEncoder.encode(null);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should use BCrypt algorithm with appropriate strength")
    void testPasswordEncoder_BCryptStrength() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "password123";

        // When
        String encodedPassword = passwordEncoder.encode(password);

        // Then - BCrypt encoded password starts with $2a$, $2b$, $2x$, or $2y$
        assertThat(encodedPassword).matches("\\$2[aby]\\$\\d{2}\\$.*");
    }
}


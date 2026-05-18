package com.tappy.pos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private long testExpiration;
    private long testRefreshExpiration;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        String testSecret = "thisis256bitlongsecretkeyfortestingjwttoken1234567890123456";
        testExpiration = 86400000L; // 24 hours
        testRefreshExpiration = 604800000L; // 7 days
        
        // Set private fields using correct field names
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpiration);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", testRefreshExpiration);
    }

    @Test
    @DisplayName("Should generate access token successfully")
    void testGenerateTokenWithRolesAndFeatures_Success() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("ADMIN", "USER");
        List<String> features = List.of("READ", "WRITE");
        boolean isMasterUser = true;

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, roles, features, isMasterUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should generate refresh token successfully")
    void testGenerateRefreshToken_Success() {
        // Given & When
        String token = jwtTokenProvider.generateRefreshToken();

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void testGetUsernameFromToken_Success() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("ADMIN");
        List<String> features = List.of("READ");
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, roles, features, true);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should validate token expiration")
    void testGetTokenExpirationMs() {
        // When
        long expiration = jwtTokenProvider.getTokenExpirationMs();

        // Then
        assertThat(expiration).isEqualTo(testExpiration);
    }

    @Test
    @DisplayName("Should get refresh token expiration")
    void testGetRefreshTokenExpirationMs() {
        // When
        long expiration = jwtTokenProvider.getRefreshTokenExpirationMs();

        // Then
        assertThat(expiration).isEqualTo(testRefreshExpiration);
    }

    @Test
    @DisplayName("Should validate token with valid token")
    void testValidateToken_Valid() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
            username, 
            List.of("USER"), 
            List.of("READ"), 
            false
        );

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should fail validation with invalid token")
    void testValidateToken_Invalid() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should fail validation with empty token")
    void testValidateToken_Empty() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle token with empty roles and features")
    void testGenerateTokenWithEmptyRolesAndFeatures() {
        // Given
        String username = "testuser";
        List<String> emptyRoles = List.of();
        List<String> emptyFeatures = List.of();

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
            username, 
            emptyRoles, 
            emptyFeatures, 
            false
        );

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate token with single role and feature")
    void testGenerateTokenWithSingleRoleAndFeature() {
        // Given
        String username = "singleuser";
        List<String> singleRole = List.of("VIEWER");
        List<String> singleFeature = List.of("DASHBOARD");

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
            username, 
            singleRole, 
            singleFeature, 
            true
        );

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testGenerateTokenWithSpecialCharactersInUsername() {
        // Given
        String username = "test.user@example.com";
        List<String> roles = List.of("USER");
        List<String> features = List.of("READ");

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
            username, 
            roles, 
            features, 
            false
        );

        // Then
        assertThat(token).isNotNull();
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateTokenForDifferentUsers() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        List<String> roles = List.of("USER");
        List<String> features = List.of("READ");

        // When
        String token1 = jwtTokenProvider.generateTokenWithRolesAndFeatures(user1, roles, features, false);
        String token2 = jwtTokenProvider.generateTokenWithRolesAndFeatures(user2, roles, features, false);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void testExtractRolesFromToken() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("ADMIN", "USER");
        List<String> features = List.of("READ");
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, roles, features, true);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should extract features from token")
    void testExtractFeaturesFromToken() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("USER");
        List<String> features = List.of("READ", "WRITE", "DELETE");
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, roles, features, false);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
        assertThat(isValid).isTrue();
    }

    // ==================== Additional Comprehensive Tests ====================

    @Test
    @DisplayName("Should generate simple token with just username")
    void testGenerateToken_SimpleUsername() {
        // Given
        String username = "john.doe@example.com";

        // When
        String token = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract claims from valid token")
    void testGetClaimsFromToken_Success() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // When
        io.jsonwebtoken.Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void testGetExpirationDateFromToken_Success() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // When
        java.util.Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate.after(new java.util.Date())).isTrue();
    }

    @Test
    @DisplayName("Should return false for null token validation")
    void testValidateToken_NullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty token validation")
    void testValidateToken_EmptyToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for malformed token")
    void testValidateToken_MalformedToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("not.a.valid.jwt.format");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should check if token is expired")
    void testIsTokenExpired_FreshToken() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // When
        Boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should return true for isTokenExpired on invalid token")
    void testIsTokenExpired_InvalidToken() {
        // When
        Boolean isExpired = jwtTokenProvider.isTokenExpired("invalid.token");

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should extract roles list from token")
    void testGetRolesFromToken_Success() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("ADMIN", "MANAGER");
        String token = jwtTokenProvider.generateTokenWithRoles(username, roles, false);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should return empty list when no roles in token")
    void testGetRolesFromToken_NoRoles() {
        // Given
        String token = jwtTokenProvider.generateToken("testuser");

        // When
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("Should extract features list from token")
    void testGetFeaturesFromToken_Success() {
        // Given
        String username = "testuser";
        List<String> features = List.of("READ", "WRITE", "DELETE");
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, List.of(), features, false);

        // When
        List<String> extractedFeatures = jwtTokenProvider.getFeaturesFromToken(token);

        // Then
        assertThat(extractedFeatures).containsExactlyInAnyOrderElementsOf(features);
    }

    @Test
    @DisplayName("Should return empty list when no features in token")
    void testGetFeaturesFromToken_NoFeatures() {
        // Given
        String token = jwtTokenProvider.generateToken("testuser");

        // When
        List<String> features = jwtTokenProvider.getFeaturesFromToken(token);

        // Then
        assertThat(features).isEmpty();
    }

    @Test
    @DisplayName("Should extract master user flag from token")
    void testIsMasterUserFromToken_True() {
        // Given
        String username = "admin@example.com";
        String token = jwtTokenProvider.generateTokenWithRoles(username, List.of("ADMIN"), true);

        // When
        Boolean isMasterUser = jwtTokenProvider.isMasterUserFromToken(token);

        // Then
        assertThat(isMasterUser).isTrue();
    }

    @Test
    @DisplayName("Should extract master user flag as false")
    void testIsMasterUserFromToken_False() {
        // Given
        String username = "user@example.com";
        String token = jwtTokenProvider.generateTokenWithRoles(username, List.of("USER"), false);

        // When
        Boolean isMasterUser = jwtTokenProvider.isMasterUserFromToken(token);

        // Then
        assertThat(isMasterUser).isFalse();
    }

    @Test
    @DisplayName("Should return false when isMasterUser not present in token")
    void testIsMasterUserFromToken_Missing() {
        // Given
        String token = jwtTokenProvider.generateToken("testuser");

        // When
        Boolean isMasterUser = jwtTokenProvider.isMasterUserFromToken(token);

        // Then
        assertThat(isMasterUser).isFalse();
    }

    @Test
    @DisplayName("Should extract role string from token")
    void testGetRoleFromToken_Success() {
        // Given
        String username = "testuser";
        String role = "SUPER_ADMIN";
        String token = jwtTokenProvider.generateTokenWithClaims(username, Map.of("role", role));

        // When
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Should handle tokens with special characters in username")
    void testGenerateToken_SpecialCharactersUsername() {
        // Given
        String username = "user+special.name@example.co.uk";

        // When
        String token = jwtTokenProvider.generateToken(username);
        String extracted = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extracted).isEqualTo(username);
    }

    @Test
    @DisplayName("Should handle very long role and feature lists")
    void testGenerateTokenWithRolesAndFeatures_LargeLists() {
        // Given
        String username = "testuser";
        List<String> manyRoles = List.of("ROLE1", "ROLE2", "ROLE3", "ROLE4", "ROLE5", "ROLE6", "ROLE7", "ROLE8");
        List<String> manyFeatures = List.of("FEAT1", "FEAT2", "FEAT3", "FEAT4", "FEAT5", "FEAT6", "FEAT7", "FEAT8", "FEAT9", "FEAT10");

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, manyRoles, manyFeatures, true);

        // Then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getRolesFromToken(token)).hasSize(8);
        assertThat(jwtTokenProvider.getFeaturesFromToken(token)).hasSize(10);
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateToken_DifferentTokensPerCall() throws InterruptedException {
        // Given
        String username = "testuser";

        // When
        String token1 = jwtTokenProvider.generateToken(username);
        Thread.sleep(1000); // Add small delay to ensure different timestamps
        String token2 = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate UUID refresh token")
    void testGenerateRefreshToken_UUID() {
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken).hasSize(36); // UUID with hyphens
    }

    @Test
    @DisplayName("Should generate different UUID refresh tokens")
    void testGenerateRefreshToken_DifferentTokens() {
        // When
        String token1 = jwtTokenProvider.generateRefreshToken();
        String token2 = jwtTokenProvider.generateRefreshToken();

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate valid JWT refresh token with username")
    void testGenerateRefreshToken_WithUsername() {
        // Given
        String username = "testuser@example.com";

        // When
        String token = jwtTokenProvider.generateRefreshToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateToken_ValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should handle token with complex claims")
    void testGenerateTokenWithClaims_ComplexClaims() {
        // Given
        String username = "testuser";
        Map<String, Object> claims = new HashMap<>();
        claims.put("customString", "value");
        claims.put("customNumber", 42);
        claims.put("customBoolean", true);

        // When
        String token = jwtTokenProvider.generateTokenWithClaims(username, claims);

        // Then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        io.jsonwebtoken.Claims extractedClaims = jwtTokenProvider.getClaimsFromToken(token);
        assertThat(extractedClaims.get("customString")).isEqualTo("value");
        assertThat(extractedClaims.get("customNumber")).isEqualTo(42);
        assertThat(extractedClaims.get("customBoolean")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle empty roles and features lists")
    void testGenerateTokenWithRolesAndFeatures_EmptyLists() {
        // Given
        String username = "testuser";
        List<String> emptyRoles = List.of();
        List<String> emptyFeatures = List.of();

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, emptyRoles, emptyFeatures, false);

        // Then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getRolesFromToken(token)).isEmpty();
        assertThat(jwtTokenProvider.getFeaturesFromToken(token)).isEmpty();
    }

    @Test
    @DisplayName("Should have correct JWT structure")
    void testGenerateToken_JwtStructure() {
        // Given
        String username = "testuser";

        // When
        String token = jwtTokenProvider.generateToken(username);
        String[] parts = token.split("\\.");

        // Then - JWT should have exactly 3 parts (header.payload.signature)
        assertThat(parts).hasSize(3);
        // Each part should be base64 encoded
        for (String part : parts) {
            assertThat(part).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should validate token with all claim types")
    void testValidateToken_AllClaimTypes() {
        // Given
        String username = "testuser";
        List<String> roles = List.of("ADMIN");
        List<String> features = List.of("READ");

        // When
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(username, roles, features, true);
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
        assertThat(jwtTokenProvider.getRolesFromToken(token)).contains("ADMIN");
        assertThat(jwtTokenProvider.getFeaturesFromToken(token)).contains("READ");
        assertThat(jwtTokenProvider.isMasterUserFromToken(token)).isTrue();
    }

    // ── generateTokenWithSession ───────────────────────────────────────────────

    @Test
    @DisplayName("generateTokenWithSession: embeds sessionId and extractable via getSessionIdFromToken")
    void testGenerateTokenWithSession_Success() {
        String token = jwtTokenProvider.generateTokenWithSession(
                "user1", List.of("SHOP_OWNER"), List.of("ORDER"), false, "sess-123");

        assertThat(jwtTokenProvider.getSessionIdFromToken(token)).isEqualTo("sess-123");
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("user1");
    }

    @Test
    @DisplayName("generateTokenWithSession: embeds shopType and tenantId when provided")
    void testGenerateTokenWithSession_WithShopTypeAndTenant() {
        String token = jwtTokenProvider.generateTokenWithSession(
                "user1", List.of("SHOP_OWNER"), List.of("ORDER"), false, "sess-xyz", "PAWN_SHOP", "shop1");

        assertThat(jwtTokenProvider.getSessionIdFromToken(token)).isEqualTo("sess-xyz");
        assertThat(jwtTokenProvider.getTenantIdsFromToken(token)).contains("shop1");
    }

    @Test
    @DisplayName("getTenantIdsFromToken: returns empty list when no tid claim")
    void testGetTenantIdsFromToken_NoTid() {
        String token = jwtTokenProvider.generateToken("user1");

        assertThat(jwtTokenProvider.getTenantIdsFromToken(token)).isEmpty();
    }

    @Test
    @DisplayName("getSessionIdFromToken: returns null when no sid claim")
    void testGetSessionIdFromToken_NoSid() {
        String token = jwtTokenProvider.generateToken("user1");

        assertThat(jwtTokenProvider.getSessionIdFromToken(token)).isNull();
    }

    @Test
    @DisplayName("generateTokenWithRolesAndFeatures: embeds shopType and tenantId")
    void testGenerateTokenWithRolesAndFeatures_WithTenantId() {
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                "user1", List.of("SHOP_OWNER"), List.of("ORDER"), false, "GENERAL", "shop42");

        assertThat(jwtTokenProvider.getTenantIdsFromToken(token)).contains("shop42");
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("user1");
    }

    @Test
    @DisplayName("generateRefreshToken no-arg: generates UUID (opaque token, not JWT)")
    void testGenerateRefreshToken_NoArg() {
        String token = jwtTokenProvider.generateRefreshToken();

        assertThat(token).isNotBlank();
        // UUID format: 8-4-4-4-12 hex groups
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("generateTokenWithRolesAndFeatures 5-param: shopType only, no tenantId")
    void generateTokenWithRolesAndFeatures_FiveParam_DelegatesToSixParam() {
        String token = jwtTokenProvider.generateTokenWithRolesAndFeatures(
                "shopuser", List.of("SHOP_OWNER"), List.of("ORDER"), false, "GENERAL_STORE");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("shopuser");
        assertThat(jwtTokenProvider.getTenantIdsFromToken(token)).isEmpty();
    }

    @Test
    @DisplayName("generateTokenWithSession 6-param: shopType only, no tenantId")
    void generateTokenWithSession_SixParam_DelegatesToSevenParam() {
        String token = jwtTokenProvider.generateTokenWithSession(
                "shopuser", List.of("SHOP_OWNER"), List.of("ORDER"), false, "sess-456", "PAWN_SHOP");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getSessionIdFromToken(token)).isEqualTo("sess-456");
        assertThat(jwtTokenProvider.getTenantIdsFromToken(token)).isEmpty();
    }

    @Test
    @DisplayName("validateToken: returns false for token signed with wrong secret (SignatureException)")
    void validateToken_WrongSignature_ReturnsFalse() {
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "jwtSecret",
                "differentSecretKeyThatIs256BitsLongForTestingJWT567890123456XYZ");
        ReflectionTestUtils.setField(otherProvider, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(otherProvider, "refreshTokenExpirationMs", 604800000L);

        String tokenFromOtherSecret = otherProvider.generateToken("testuser");

        assertThat(jwtTokenProvider.validateToken(tokenFromOtherSecret)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for already-expired token (ExpiredJwtException)")
    void validateToken_ExpiredToken_ReturnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret",
                "thisis256bitlongsecretkeyfortestingjwttoken1234567890123456");
        ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -86400000L);
        ReflectionTestUtils.setField(expiredProvider, "refreshTokenExpirationMs", 604800000L);

        String expiredToken = expiredProvider.generateToken("testuser");

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }
}


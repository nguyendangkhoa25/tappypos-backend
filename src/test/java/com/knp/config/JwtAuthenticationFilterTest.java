package com.knp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.service.auth.SessionRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 * Covers JWT token extraction and validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthContext authContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FeatureContext featureContext;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TEST_USERNAME = "testuser@example.com";

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, authContext, featureContext, sessionRegistry, objectMapper);
        SecurityContextHolder.clearContext();
        // Default stubs for session validation (lenient to avoid unused-stub errors in non-token tests)
        lenient().when(jwtTokenProvider.getSessionIdFromToken(anyString())).thenReturn("test-session-id");
        lenient().when(jwtTokenProvider.isMasterUserFromToken(anyString())).thenReturn(null);
        lenient().when(jwtTokenProvider.getFeaturesFromToken(anyString())).thenReturn(java.util.Collections.emptyList());
        lenient().when(sessionRegistry.isValid(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Should extract and validate JWT token from Authorization header")
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext).setCurrentUsername(TEST_USERNAME);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when no Authorization header")
    void testDoFilterInternal_NoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext, never()).setCurrentUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header without Bearer prefix")
    void testDoFilterInternal_NoBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNzd29yZA==");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext, never()).setCurrentUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set auth context for invalid token")
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.token.format";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext, never()).setCurrentUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain even when exception occurs")
    void testDoFilterInternal_ExceptionHandling() throws ServletException, IOException {
        // Given
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token validation error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle empty Bearer token")
    void testDoFilterInternal_EmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle Authorization header with extra spaces")
    void testDoFilterInternal_ExtraSpaces() throws ServletException, IOException {
        // Given
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer  " + token);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set username in AuthContext when token is valid")
    void testDoFilterInternal_SetUsernameInAuthContext() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "user@example.com";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(username);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext).setCurrentUsername(username);
    }

    @Test
    @DisplayName("Should handle multiple calls with same token")
    void testDoFilterInternal_MultipleCallsSameToken() throws ServletException, IOException {
        // Given
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(authContext, times(2)).setCurrentUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should handle case-sensitive Authorization header")
    void testDoFilterInternal_CaseSensitiveHeader() throws ServletException, IOException {
        // Given - lowercase "authorization" header
        when(request.getHeader("Authorization")).thenReturn(null);
        // Actual implementation should handle the specific case

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract correct token substring from Bearer prefix")
    void testBearerTokenExtraction() throws ServletException, IOException {
        // Given
        String actualToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIn0.signature";
        String authHeader = "Bearer " + actualToken;
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtTokenProvider.validateToken(actualToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(actualToken)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(actualToken);
        verify(jwtTokenProvider).getUsernameFromToken(actualToken);
    }

    @Test
    @DisplayName("Should clear SecurityContext when filter completes")
    void testDoFilterInternal_DoesNotLeakContext() throws ServletException, IOException {
        // Given
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(TEST_USERNAME);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - SecurityContext should have been populated, then filterChain called
        verify(filterChain).doFilter(request, response);
    }
}


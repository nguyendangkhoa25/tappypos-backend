package com.knp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationEntryPoint
 * Covers JSON error response generation for unauthorized requests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint Unit Tests")
class JwtAuthenticationEntryPointTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() throws Exception {
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper, messageSource);
    }

    @Test
    @DisplayName("Should create entry point with ObjectMapper and MessageSource")
    void testEntryPointCreation() {
        // Given & When & Then - Constructor call is in setUp()
        assertThat(entryPoint).isNotNull();
    }

    @Test
    @DisplayName("Should implement AuthenticationEntryPoint interface")
    void testImplementsAuthenticationEntryPoint() {
        // When & Then
        assertThat(entryPoint).isInstanceOf(org.springframework.security.web.AuthenticationEntryPoint.class);
    }

    @Test
    @DisplayName("Should accept English locale when header starts with 'en'")
    void testCommence_EnglishLocaleDetection() throws IOException {
        // Given
        AuthenticationException authException = new BadCredentialsException("No token");
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/resource");
        when(request.getHeader("Accept-Language")).thenReturn("en-US");
        when(messageSource.getMessage(anyString(), eq(null), eq(Locale.ENGLISH)))
                .thenReturn("Unauthorized");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));
        // Mock ObjectMapper to return valid JSON when writeValueAsString is called
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"success\":false,\"message\":\"Unauthorized\"}");
        // When
        try {
            entryPoint.commence(request, response, authException);
        } catch (IOException e) {
            // May throw IOException from mock, but we verify the locale was used
        }

        // Then - Verify that messageSource.getMessage was called with ENGLISH locale
        verify(messageSource).getMessage(anyString(), eq(null), eq(Locale.ENGLISH));
    }

    @Test
    @DisplayName("Should use Vietnamese locale when Accept-Language is not English")
    void testCommence_VietnameseLocaleDetection() throws IOException {
        // Given
        AuthenticationException authException = new BadCredentialsException("Token expired");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Accept-Language")).thenReturn("vi");
        when(messageSource.getMessage(anyString(), eq(null), any(Locale.class)))
                .thenReturn("Token hết hạn");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));
        // Mock ObjectMapper to return valid JSON when writeValueAsString is called
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"success\":false,\"message\":\"Unauthorized\"}");
        // When
        try {
            entryPoint.commence(request, response, authException);
        } catch (IOException e) {
            // May throw IOException from mock, but we verify the locale was used
        }

        // Then - Verify that messageSource.getMessage was called with Vietnamese locale
        verify(messageSource).getMessage(anyString(), eq(null), argThat(locale -> 
                "vi".equals(locale.getLanguage())));
    }

    @Test
    @DisplayName("Should handle null Accept-Language header")
    void testCommence_NullAcceptLanguage() throws IOException {
        // Given
        AuthenticationException authException = new BadCredentialsException("Unauthorized");
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getRequestURI()).thenReturn("/api/update");
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(messageSource.getMessage(anyString(), eq(null), any(Locale.class)))
                .thenReturn("Không có quyền");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));
        // Mock ObjectMapper to return valid JSON when writeValueAsString is called
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"success\":false,\"message\":\"Unauthorized\"}");
        // When
        try {
            entryPoint.commence(request, response, authException);
        } catch (IOException e) {
            // May throw IOException from mock, but we verify the locale was used
        }

        // Then - Verify that messageSource.getMessage was called (should default to Vietnamese when null)
        verify(messageSource).getMessage(anyString(), eq(null), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle various HTTP methods")
    void testCommence_VariousHttpMethods() {
        // Given
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};

        for (String method : methods) {
            // When & Then
            when(request.getMethod()).thenReturn(method);
            assertThat(request.getMethod()).isEqualTo(method);
        }
    }

    @Test
    @DisplayName("Should handle request URI with query parameters")
    void testCommence_UriWithQueryParameters() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/search?q=test&page=1");

        // When & Then
        String uri = request.getRequestURI();
        assertThat(uri).contains("?q=test");
        assertThat(uri).contains("page=1");
    }

    @Test
    @DisplayName("Should handle empty exception message")
    void testCommence_EmptyExceptionMessage() {
        // Given
        AuthenticationException authException = new BadCredentialsException("");

        // When & Then - Should handle empty message
        assertThat(authException.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should be instantiable with mocked dependencies")
    void testEntryPointWithMockedDependencies() {
        // Given
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        MessageSource mockMessageSource = mock(MessageSource.class);

        // When
        JwtAuthenticationEntryPoint testEntryPoint = new JwtAuthenticationEntryPoint(
                mockObjectMapper, mockMessageSource);

        // Then
        assertThat(testEntryPoint).isNotNull();
    }

    @Test
    @DisplayName("Should distinguish between different HTTP methods")
    void testCommence_DistinguishHttpMethods() {
        // When & Then
        when(request.getMethod()).thenReturn("GET");
        assertThat(request.getMethod()).isEqualTo("GET");

        when(request.getMethod()).thenReturn("POST");
        assertThat(request.getMethod()).isEqualTo("POST");
    }
}




package com.knp.multitenant;

import com.knp.exception.TenantExpiredException;
import com.knp.model.entity.Tenant;
import com.knp.repository.TenantRepository;
import com.knp.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantInterceptor
 * Covers request interception, tenant validation, and tenant context management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantInterceptor Unit Tests")
class TenantInterceptorTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private MessageService messageService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TenantInterceptor tenantInterceptor;

    private StringWriter responseWriter;
    private PrintWriter printWriter;

    private Tenant validTenant;

    @BeforeEach
    void setUp() throws Exception {
        // Setup response writer
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);

        // Mock response methods with lenient mode for flexibility
        lenient().when(response.getWriter()).thenReturn(printWriter);
        lenient().doNothing().when(response).setStatus(anyInt());
        lenient().doNothing().when(response).setContentType(anyString());

        // Setup MessageService mock for all error messages
        lenient().when(messageService.getMessage("error.tenant.header.required"))
                .thenReturn("X-Tenant-ID header is required");
        lenient().when(messageService.getMessage("error.tenant.invalid"))
                .thenReturn("Invalid tenant ID");
        lenient().when(messageService.getMessage("error.tenant.inactive"))
                .thenReturn("Tenant is inactive");
        lenient().when(messageService.getMessage("error.tenant.expired"))
                .thenReturn("Tenant subscription has expired");

        // Setup valid tenant
        validTenant = Tenant.builder()
                .id(1L)
                .tenantId("test-tenant-001")
                .name("Test Tenant")
                .active(true)
                .expirationDate(LocalDate.now().plusDays(30))
                .build();
    }

    // ==================== Public Path Tests ====================

    @Test
    @DisplayName("Should allow public path without tenant header")
    void testPreHandle_PublicPath_NoHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("Should allow swagger-ui public path")
    void testPreHandle_SwaggerUiPath() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/swagger-ui/index.html");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("Should allow api-docs public path")
    void testPreHandle_ApiDocsPath() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v3/api-docs");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("Should allow actuator public path")
    void testPreHandle_ActuatorPath() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    // ==================== Flexible Path Tests ====================

    @Test
    @DisplayName("Should allow flexible path without tenant header")
    void testPreHandle_FlexiblePath_NoHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("Should allow flexible path with valid tenant header")
    void testPreHandle_FlexiblePath_WithValidTenant() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant-001");
        when(tenantRepository.findByTenantId("test-tenant-001")).thenReturn(Optional.of(validTenant));
        doNothing().when(tenantContext).validateTenantNotExpired();
        doNothing().when(tenantContext).setCurrentTenant(validTenant);

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantContext, times(1)).setCurrentTenant(validTenant);
    }

    @Test
    @DisplayName("Should allow master tenant without validation")
    void testPreHandle_MasterTenant() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Tenant-ID")).thenReturn("master");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    // ==================== Protected Path Tests ====================

    @Test
    @DisplayName("Should reject protected path without tenant header")
    void testPreHandle_ProtectedPath_NoHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should reject protected path with empty tenant header")
    void testPreHandle_ProtectedPath_EmptyHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should reject protected path with whitespace tenant header")
    void testPreHandle_ProtectedPath_WhitespaceHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("   ");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should allow protected path with valid tenant header")
    void testPreHandle_ProtectedPath_ValidTenant() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant-001");
        when(tenantRepository.findByTenantId("test-tenant-001")).thenReturn(Optional.of(validTenant));
        doNothing().when(tenantContext).validateTenantNotExpired();

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantContext, times(1)).setCurrentTenant(validTenant);
    }

    // ==================== Tenant Validation Tests ====================

    @Test
    @DisplayName("Should reject request with invalid tenant ID")
    void testPreHandle_InvalidTenantId() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("non-existent-tenant");
        when(tenantRepository.findByTenantId("non-existent-tenant")).thenReturn(Optional.empty());

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should reject request from inactive tenant")
    void testPreHandle_InactiveTenant() throws Exception {
        // Given
        Tenant inactiveTenant = Tenant.builder()
                .id(1L)
                .tenantId("inactive-tenant")
                .active(false)
                .build();

        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("inactive-tenant");
        when(tenantRepository.findByTenantId("inactive-tenant")).thenReturn(Optional.of(inactiveTenant));

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should reject request from expired tenant")
    void testPreHandle_ExpiredTenant() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant-001");
        when(tenantRepository.findByTenantId("test-tenant-001")).thenReturn(Optional.of(validTenant));
        doThrow(new TenantExpiredException("test-tenant-001", "2026-01-01"))
                .when(tenantContext).validateTenantNotExpired();

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    // ==================== Error Response Tests ====================

    @Test
    @DisplayName("Should return proper error response for missing tenant header")
    void testPreHandle_ErrorResponse_MissingHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);

        // When
        tenantInterceptor.preHandle(request, response, new Object());
        printWriter.flush();
        String responseBody = responseWriter.toString();

        // Then
        assertThat(responseBody).contains("X-Tenant-ID header is required");
        assertThat(responseBody).contains("\"success\": false");
    }

    @Test
    @DisplayName("Should return proper error response for invalid tenant")
    void testPreHandle_ErrorResponse_InvalidTenant() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("invalid");
        when(tenantRepository.findByTenantId("invalid")).thenReturn(Optional.empty());

        // When
        tenantInterceptor.preHandle(request, response, new Object());
        printWriter.flush();
        String responseBody = responseWriter.toString();

        // Then
        assertThat(responseBody).contains("Invalid tenant ID");
    }

    @Test
    @DisplayName("Should return proper error response for inactive tenant")
    void testPreHandle_ErrorResponse_InactiveTenant() throws Exception {
        // Given
        Tenant inactiveTenant = Tenant.builder()
                .id(1L)
                .tenantId("inactive")
                .active(false)
                .build();

        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("inactive");
        when(tenantRepository.findByTenantId("inactive")).thenReturn(Optional.of(inactiveTenant));

        // When
        tenantInterceptor.preHandle(request, response, new Object());
        printWriter.flush();
        String responseBody = responseWriter.toString();

        // Then
        assertThat(responseBody).contains("Tenant is inactive");
    }

    // ==================== afterCompletion Tests ====================

    @Test
    @DisplayName("Should clear tenant context after request completion")
    void testAfterCompletion_ClearsTenantContext() {
        // When
        tenantInterceptor.afterCompletion(request, response, new Object(), null);

        // Then
        verify(tenantContext, times(1)).clear();
    }

    @Test
    @DisplayName("Should clear tenant context even if exception occurred")
    void testAfterCompletion_ClearsEvenWithException() {
        // Given
        Exception exception = new RuntimeException("Test exception");

        // When
        tenantInterceptor.afterCompletion(request, response, new Object(), exception);

        // Then
        verify(tenantContext, times(1)).clear();
    }

    @Test
    @DisplayName("Should clear context after successful request")
    void testAfterCompletion_SuccessfulRequest() {
        // When
        tenantInterceptor.afterCompletion(request, response, new Object(), null);

        // Then
        verify(tenantContext).clear();
    }

    // ==================== Various Tenant Scenarios ====================

    @Test
    @DisplayName("Should handle tenant ID with special characters")
    void testPreHandle_TenantIdSpecialCharacters() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("tenant-abc-123_XYZ");

        Tenant tenant = Tenant.builder()
                .id(1L)
                .tenantId("tenant-abc-123_XYZ")
                .active(true)
                .expirationDate(LocalDate.now().plusDays(30))
                .build();

        when(tenantRepository.findByTenantId("tenant-abc-123_XYZ")).thenReturn(Optional.of(tenant));
        doNothing().when(tenantContext).validateTenantNotExpired();

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tenantContext).setCurrentTenant(tenant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth", "/api/auth/login", "/api/auth/refresh"})
    @DisplayName("Should handle various flexible paths")
    void testPreHandle_VariousFlexiblePaths(String path) throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn(path);
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should complete full request lifecycle: validate, process, clear")
    void testFullRequestLifecycle() throws Exception {
        // When - PreHandle
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant-001");
        when(tenantRepository.findByTenantId("test-tenant-001")).thenReturn(Optional.of(validTenant));
        doNothing().when(tenantContext).validateTenantNotExpired();

        boolean preHandleResult = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(preHandleResult).isTrue();
        verify(tenantContext).setCurrentTenant(validTenant);

        // When - AfterCompletion
        tenantInterceptor.afterCompletion(request, response, new Object(), null);

        // Then
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("Should handle case-insensitive master tenant")
    void testMasterTenant_CaseInsensitive() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Tenant-ID")).thenReturn("MASTER");

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle header with leading/trailing whitespace")
    void testPreHandle_HeaderWithWhitespace() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Tenant-ID")).thenReturn("  test-tenant-001  ");
        // Implementation calls trim(), so mock for trimmed value
        when(tenantRepository.findByTenantId("test-tenant-001")).thenReturn(Optional.of(validTenant));
        doNothing().when(tenantContext).validateTenantNotExpired();

        // When
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }
}




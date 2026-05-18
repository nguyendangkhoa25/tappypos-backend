package com.tappy.pos.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantLogFilter
 * Covers filtering logs to only accept tenant-specific logs
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantLogFilter Unit Tests")
class TenantLogFilterTest {

    @Mock
    private ILoggingEvent loggingEvent;

    private TenantLogFilter tenantLogFilter;

    @BeforeEach
    void setUp() {
        tenantLogFilter = new TenantLogFilter();
    }

    @Test
    @DisplayName("Should accept logs with valid tenant ID")
    void testDecide_AcceptWithValidTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should deny logs with no tenant ID")
    void testDecide_DenyWithoutTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny logs with empty tenant ID")
    void testDecide_DenyWithEmptyTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny logs with 'default' tenant ID")
    void testDecide_DenyWithDefaultTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "default");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should accept logs with numeric tenant ID")
    void testDecide_AcceptWithNumericTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "12345");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept logs with alphanumeric tenant ID")
    void testDecide_AcceptWithAlphanumericTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-abc123-xyz");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept logs with UUID format tenant ID")
    void testDecide_AcceptWithUuidTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "550e8400-e29b-41d4-a716-446655440000");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should deny logs with null tenant ID")
    void testDecide_DenyWithNullTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", null);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should handle MDC map with multiple keys")
    void testDecide_MultipleKeysInMdc() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-001");
        mdcMap.put("userId", "user-123");
        mdcMap.put("requestId", "req-456");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept various non-default tenant IDs")
    void testDecide_VariousNonDefaultTenantIds() {
        // Given
        String[] tenantIds = {"tenant-1", "tenant-2", "shop-001", "app-123", "service-abc"};

        for (String tenantId : tenantIds) {
            Map<String, String> mdcMap = new HashMap<>();
            mdcMap.put("tenantId", tenantId);
            when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

            // When
            FilterReply result = tenantLogFilter.decide(loggingEvent);

            // Then
            assertThat(result).isEqualTo(FilterReply.ACCEPT);
        }
    }

    @Test
    @DisplayName("Should be case-sensitive for 'default'")
    void testDecide_DefaultCaseSensitive() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "DEFAULT");  // uppercase
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then - Should ACCEPT because it's not exactly "default"
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should handle tenant ID with spaces")
    void testDecide_TenantIdWithSpaces() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant 001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept long tenant ID strings")
    void testDecide_LongTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "very-long-tenant-id-with-many-characters-for-testing-purposes-001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should handle special characters in tenant ID")
    void testDecide_TenantIdWithSpecialChars() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant_001-shop@branch");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = tenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }
}


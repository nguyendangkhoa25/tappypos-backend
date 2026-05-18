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
 * Unit tests for NonTenantLogFilter
 * Covers filtering logs to only accept non-tenant logs (default/master)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NonTenantLogFilter Unit Tests")
class NonTenantLogFilterTest {

    @Mock
    private ILoggingEvent loggingEvent;

    private NonTenantLogFilter nonTenantLogFilter;

    @BeforeEach
    void setUp() {
        nonTenantLogFilter = new NonTenantLogFilter();
    }

    @Test
    @DisplayName("Should accept logs without tenant ID")
    void testDecide_AcceptWithoutTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept logs with 'default' tenant ID")
    void testDecide_AcceptWithDefaultTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "default");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept logs with empty tenant ID")
    void testDecide_AcceptWithEmptyTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should accept logs with null tenant ID")
    void testDecide_AcceptWithNullTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", null);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("Should deny logs with tenant ID other than default")
    void testDecide_DenyWithTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny logs with numeric tenant ID")
    void testDecide_DenyWithNumericTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "12345");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny logs with UUID format tenant ID")
    void testDecide_DenyWithUuidTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "550e8400-e29b-41d4-a716-446655440000");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should be case-sensitive for 'default'")
    void testDecide_DefaultCaseSensitive() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "DEFAULT");  // uppercase
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then - Should DENY because it's not exactly "default"
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should handle MDC with multiple keys")
    void testDecide_MultipleKeysWithTenant() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-001");
        mdcMap.put("userId", "user-123");
        mdcMap.put("requestId", "req-456");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny various non-default tenant IDs")
    void testDecide_VariousNonDefaultTenantIds() {
        // Given
        String[] tenantIds = {"tenant-1", "shop-001", "app-123", "service-abc", "org-xyz"};

        for (String tenantId : tenantIds) {
            Map<String, String> mdcMap = new HashMap<>();
            mdcMap.put("tenantId", tenantId);
            when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

            // When
            FilterReply result = nonTenantLogFilter.decide(loggingEvent);

            // Then
            assertThat(result).isEqualTo(FilterReply.DENY);
        }
    }

    @Test
    @DisplayName("Should accept logs with only 'default' among other values")
    void testDecide_OtherKeysDefaultTenant() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "default");
        mdcMap.put("userId", "user-123");
        mdcMap.put("requestId", "req-456");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
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
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should handle default with spaces (not default)")
    void testDecide_DefaultWithSpaces() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "default ");  // space after
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should deny long non-default tenant IDs")
    void testDecide_LongTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "very-long-tenant-id-with-many-characters-for-testing-purposes-001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        FilterReply result = nonTenantLogFilter.decide(loggingEvent);

        // Then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("Should filter correctly for both tenant and non-tenant logs")
    void testDecide_MixedTenantAndNonTenantLogs() {
        // Test non-tenant log
        Map<String, String> mdcMap1 = new HashMap<>();
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap1);
        assertThat(nonTenantLogFilter.decide(loggingEvent)).isEqualTo(FilterReply.ACCEPT);

        // Test tenant log
        Map<String, String> mdcMap2 = new HashMap<>();
        mdcMap2.put("tenantId", "tenant-001");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap2);
        assertThat(nonTenantLogFilter.decide(loggingEvent)).isEqualTo(FilterReply.DENY);

        // Test default log
        Map<String, String> mdcMap3 = new HashMap<>();
        mdcMap3.put("tenantId", "default");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap3);
        assertThat(nonTenantLogFilter.decide(loggingEvent)).isEqualTo(FilterReply.ACCEPT);
    }
}


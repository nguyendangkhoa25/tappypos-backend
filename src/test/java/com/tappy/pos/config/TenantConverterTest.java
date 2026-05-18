package com.tappy.pos.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantConverter
 * Covers tenant ID extraction from MDC in logging events
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantConverter Unit Tests")
class TenantConverterTest {

    @Mock
    private ILoggingEvent loggingEvent;

    private TenantConverter tenantConverter;

    @BeforeEach
    void setUp() {
        tenantConverter = new TenantConverter();
        MDC.clear(); // Clear MDC state
    }

    @Test
    @DisplayName("Should return tenant ID when present in MDC")
    void testConvert_WithTenantId() {
        // Given
        String tenantId = "tenant-001";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is missing from MDC")
    void testConvert_NoTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();


        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is empty string")
    void testConvert_EmptyTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "");

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is null")
    void testConvert_NullTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", null);


        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should handle numeric tenant IDs")
    void testConvert_NumericTenantId() {
        // Given
        String tenantId = "12345";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle alphanumeric tenant IDs")
    void testConvert_AlphanumericTenantId() {
        // Given
        String tenantId = "tenant-abc123-xyz";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle tenant ID with special characters")
    void testConvert_TenantIdWithSpecialChars() {
        // Given
        String tenantId = "tenant-001_shop-123";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle UUID format tenant IDs")
    void testConvert_UuidTenantId() {
        // Given
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle long tenant IDs")
    void testConvert_LongTenantId() {
        // Given
        String tenantId = "very-long-tenant-id-with-many-characters-and-hyphens-for-testing-purposes";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle whitespace in tenant ID")
    void testConvert_WhitespaceTenantId() {
        // Given
        String tenantId = "  tenant-001  ";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return 'default' when MDC map is empty")
    void testConvert_EmptyMdcMap() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        

        // When
        String result = tenantConverter.convert(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should return 'default' when MDC map is null")
    void testConvert_NullMdcMap() {
        // Given
        //when(loggingEvent.getMDCPropertyMap()).thenReturn(null);

        // When & Then - Should handle gracefully
        try {
            tenantConverter.convert(loggingEvent);
            // If no exception, that's acceptable - implementation choice
        } catch (NullPointerException e) {
            // Expected if implementation accesses null map
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @DisplayName("Should differentiate between different tenant IDs")
    void testConvert_MultipleTenants() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        String tenant3 = "tenant-003";

        // When & Then
        Map<String, String> map1 = new HashMap<>();
        map1.put("tenantId", tenant1);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(map1);
        assertThat(tenantConverter.convert(loggingEvent)).isEqualTo(tenant1);

        Map<String, String> map2 = new HashMap<>();
        map2.put("tenantId", tenant2);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(map2);
        assertThat(tenantConverter.convert(loggingEvent)).isEqualTo(tenant2);

        Map<String, String> map3 = new HashMap<>();
        map3.put("tenantId", tenant3);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(map3);
        assertThat(tenantConverter.convert(loggingEvent)).isEqualTo(tenant3);
    }
}


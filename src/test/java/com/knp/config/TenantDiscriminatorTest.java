package com.knp.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
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
 * Unit tests for TenantDiscriminator
 * Covers log routing based on tenant ID from MDC
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantDiscriminator Unit Tests")
class TenantDiscriminatorTest {

    @Mock
    private ILoggingEvent loggingEvent;

    private TenantDiscriminator discriminator;

    @BeforeEach
    void setUp() {
        discriminator = new TenantDiscriminator();
    }

    @Test
    @DisplayName("Should return tenant ID when present in MDC")
    void testGetDiscriminatingValue_WithTenantId() {
        // Given
        String tenantId = "tenant-001";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is missing from MDC")
    void testGetDiscriminatingValue_NoTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is empty string")
    void testGetDiscriminatingValue_EmptyTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should return 'default' when tenant ID is null")
    void testGetDiscriminatingValue_NullTenantId() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", null);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("Should have correct key value")
    void testGetKey() {
        // When
        String key = discriminator.getKey();

        // Then
        assertThat(key).isEqualTo("tenantId");
    }

    @Test
    @DisplayName("Should have correct default value")
    void testGetDefaultValue() {
        // When
        String defaultValue = discriminator.getDefaultValue();

        // Then
        assertThat(defaultValue).isEqualTo("default");
    }

    @Test
    @DisplayName("Should allow setting custom key")
    void testSetKey() {
        // When
        discriminator.setKey("customKey");

        // Then
        assertThat(discriminator.getKey()).isEqualTo("customKey");
    }

    @Test
    @DisplayName("Should allow setting custom default value")
    void testSetDefaultValue() {
        // When
        discriminator.setDefaultValue("customDefault");

        // Then
        assertThat(discriminator.getDefaultValue()).isEqualTo("customDefault");
    }

    @Test
    @DisplayName("Should use custom key to extract value from MDC")
    void testGetDiscriminatingValue_CustomKey() {
        // Given
        discriminator.setKey("customTenantKey");
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("customTenantKey", "tenant-custom");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("tenant-custom");
    }

    @Test
    @DisplayName("Should use custom default value when tenant ID is missing")
    void testGetDiscriminatingValue_CustomDefaultValue() {
        // Given
        discriminator.setDefaultValue("customDefault");
        Map<String, String> mdcMap = new HashMap<>();
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("customDefault");
    }

    @Test
    @DisplayName("Should handle numeric tenant IDs")
    void testGetDiscriminatingValue_NumericTenantId() {
        // Given
        String tenantId = "12345";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should handle UUID format tenant IDs")
    void testGetDiscriminatingValue_UuidTenantId() {
        // Given
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should start discriminator")
    void testStart() {
        // When
        discriminator.start();

        // Then - should mark as started
        assertThat(discriminator.isStarted()).isTrue();
    }

    @Test
    @DisplayName("Should differentiate between different tenant IDs")
    void testGetDiscriminatingValue_MultipleTenants() {
        // Given
        String[] tenantIds = {"tenant-001", "tenant-002", "tenant-003"};

        for (String tenantId : tenantIds) {
            Map<String, String> mdcMap = new HashMap<>();
            mdcMap.put("tenantId", tenantId);
            when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

            // When
            String result = discriminator.getDiscriminatingValue(loggingEvent);

            // Then
            assertThat(result).isEqualTo(tenantId);
        }
    }

    @Test
    @DisplayName("Should handle MDC with multiple keys")
    void testGetDiscriminatingValue_MultipleKeys() {
        // Given
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", "tenant-001");
        mdcMap.put("userId", "user-123");
        mdcMap.put("requestId", "req-456");
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo("tenant-001");
    }

    @Test
    @DisplayName("Should handle long tenant IDs")
    void testGetDiscriminatingValue_LongTenantId() {
        // Given
        String tenantId = "very-long-tenant-id-with-many-characters-for-testing-purposes-001";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should preserve case of tenant ID")
    void testGetDiscriminatingValue_PreserveCase() {
        // Given
        String tenantId = "TenantID-ABC123";
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("tenantId", tenantId);
        when(loggingEvent.getMDCPropertyMap()).thenReturn(mdcMap);

        // When
        String result = discriminator.getDiscriminatingValue(loggingEvent);

        // Then
        assertThat(result).isEqualTo(tenantId);
    }
}


package com.knp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AppConfig
 * Covers bean creation and configuration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppConfig Unit Tests")
class AppConfigTest {

    private final AppConfig appConfig = new AppConfig();

    @Test
    @DisplayName("Should create RestTemplate bean")
    void testRestTemplate_Created() {
        // When
        RestTemplate restTemplate = appConfig.restTemplate();

        // Then
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("Should create ObjectMapper bean")
    void testObjectMapper_Created() {
        // When
        ObjectMapper objectMapper = appConfig.objectMapper();

        // Then
        assertThat(objectMapper).isNotNull();
    }

    @Test
    @DisplayName("ObjectMapper should have JavaTimeModule registered")
    void testObjectMapper_JavaTimeModuleRegistered() throws Exception {
        // When
        ObjectMapper objectMapper = appConfig.objectMapper();
        LocalDateTime now = LocalDateTime.now();
        String json = objectMapper.writeValueAsString(now);

        // Then
        assertThat(json).isNotNull();
        // Should not be a timestamp (would be number), should be string
        assertThat(json).isNotEmpty();
        assertThat(json).doesNotStartWith("[");
    }

    @Test
    @DisplayName("ObjectMapper should disable writing dates as timestamps")
    void testObjectMapper_DisableWriteDatesAsTimestamps() {
        // When
        ObjectMapper objectMapper = appConfig.objectMapper();

        // Then
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                .isFalse();
    }

    @Test
    @DisplayName("ObjectMapper should serialize LocalDateTime as ISO string")
    void testObjectMapper_SerializeLocalDateTimeAsIso() throws Exception {
        // Given
        ObjectMapper objectMapper = appConfig.objectMapper();
        LocalDateTime testTime = LocalDateTime.of(2026, 3, 15, 10, 30, 45);

        // When
        String json = objectMapper.writeValueAsString(testTime);

        // Then
        assertThat(json).contains("2026-03-15");
    }

    @Test
    @DisplayName("ObjectMapper should deserialize ISO date string to LocalDateTime")
    void testObjectMapper_DeserializeIsoToLocalDateTime() throws Exception {
        // Given
        ObjectMapper objectMapper = appConfig.objectMapper();
        String json = "\"2026-03-15T10:30:45\"";

        // When
        LocalDateTime result = objectMapper.readValue(json, LocalDateTime.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getMonthValue()).isEqualTo(3);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("Multiple calls to restTemplate() should create new instances")
    void testRestTemplate_NewInstancePerCall() {
        // When
        RestTemplate rt1 = appConfig.restTemplate();
        RestTemplate rt2 = appConfig.restTemplate();

        // Then
        assertThat(rt1).isNotNull();
        assertThat(rt2).isNotNull();
        // Different instances (not singleton beans in this context)
        assertThat(rt1).isNotEqualTo(rt2);
    }

    @Test
    @DisplayName("Multiple calls to objectMapper() should create new instances")
    void testObjectMapper_NewInstancePerCall() {
        // When
        ObjectMapper om1 = appConfig.objectMapper();
        ObjectMapper om2 = appConfig.objectMapper();

        // Then
        assertThat(om1).isNotNull();
        assertThat(om2).isNotNull();
        // Different instances
        assertThat(om1).isNotEqualTo(om2);
    }

    @Test
    @DisplayName("ObjectMapper should handle null values")
    void testObjectMapper_HandleNullValues() throws Exception {
        // Given
        ObjectMapper objectMapper = appConfig.objectMapper();

        // When
        String json = objectMapper.writeValueAsString(null);

        // Then
        assertThat(json).isEqualTo("null");
    }
}


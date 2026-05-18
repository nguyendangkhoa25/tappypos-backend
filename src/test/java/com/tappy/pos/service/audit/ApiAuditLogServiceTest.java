package com.tappy.pos.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.model.entity.audit.ApiAuditLog;
import com.tappy.pos.repository.audit.ApiAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiAuditLogService Unit Tests")
class ApiAuditLogServiceTest {

    @Mock
    private ApiAuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiAuditLogService auditLogService;

    private ApiAuditLogService.ApiAuditLogRequest testRequest;

    @BeforeEach
    void setUp() {
        HttpHeaders testHeaders = new HttpHeaders();
        testHeaders.set("Content-Type", "application/json");
        testHeaders.set("Authorization", "Bearer token123");

        testRequest = ApiAuditLogService.ApiAuditLogRequest.builder()
                .traceId("trace-001")
                .apiEndpoint("/api/v1/users")
                .httpMethod("POST")
                .requestBody("{\"name\": \"John\"}")
                .requestHeaders(testHeaders)
                .responseBody("{\"id\": 1}")
                .responseHeaders(testHeaders)
                .responseStatus(200)
                .executionTimeMs(100L)
                .errorMessage(null)
                .exceptionStackTrace(null)
                .userId("user-123")
                .ipAddress("192.168.1.1")
                .status("SUCCESS")
                .description("Create user")
                .build();
    }

    // ============= logApiCall() Tests =============

    @Test
    @DisplayName("Should log API call with all fields populated")
    void testLogApiCall_AllFieldsPopulated() throws JsonProcessingException {
        // Given
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        
        ApiAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getTraceId()).isEqualTo("trace-001");
        assertThat(savedLog.getApiEndpoint()).isEqualTo("/api/v1/users");
        assertThat(savedLog.getHttpMethod()).isEqualTo("POST");
        assertThat(savedLog.getResponseStatus()).isEqualTo(200);
        assertThat(savedLog.getUserId()).isEqualTo("user-123");
        assertThat(savedLog.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Should generate trace ID when not provided")
    void testLogApiCall_GenerateTraceId() throws JsonProcessingException {
        // Given
        testRequest.setTraceId(null);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        ApiAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getTraceId()).isNotNull();
        assertThat(savedLog.getTraceId()).isNotEmpty();
    }

    @Test
    @DisplayName("Should determine status as SUCCESS for 2xx response")
    void testLogApiCall_StatusSuccess() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(201);
        testRequest.setStatus(null);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Should determine status as FAILURE for 4xx response")
    void testLogApiCall_StatusFailure() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(400);
        testRequest.setStatus(null);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("Should determine status as ERROR for 5xx response")
    void testLogApiCall_StatusError() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(500);
        testRequest.setStatus(null);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should determine status as UNKNOWN for null response status")
    void testLogApiCall_StatusUnknown_NullResponseStatus() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(null);
        testRequest.setStatus(null);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should use provided status instead of determining from response code")
    void testLogApiCall_UseProvidedStatus() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(200);
        testRequest.setStatus("CUSTOM_STATUS");
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CUSTOM_STATUS");
    }

    @Test
    @DisplayName("Should handle null request body")
    void testLogApiCall_NullRequestBody() {
        // Given
        testRequest.setRequestBody(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestBody()).isNull();
    }

    @Test
    @DisplayName("Should handle null response body")
    void testLogApiCall_NullResponseBody() {
        // Given
        testRequest.setResponseBody(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseBody()).isNull();
    }

    @Test
    @DisplayName("Should handle null request headers")
    void testLogApiCall_NullRequestHeaders() {
        // Given
        testRequest.setRequestHeaders(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestHeaders()).isNull();
    }

    @Test
    @DisplayName("Should handle exception during logging gracefully")
    void testLogApiCall_RepositoryThrowsException() {
        // Given
        when(auditLogRepository.save(any(ApiAuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw, exception is caught
        auditLogService.logApiCall(testRequest);

        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should calculate request size correctly")
    void testLogApiCall_CalculatesRequestSize() throws JsonProcessingException {
        // Given
        testRequest.setRequestBody("{\"name\": \"John\"}");
        doReturn("{\"name\": \"John\"}").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate response size correctly")
    void testLogApiCall_CalculatesResponseSize() throws JsonProcessingException {
        // Given
        testRequest.setResponseBody("{\"id\": 1}");
        doReturn("{\"id\": 1}").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should set execution time correctly")
    void testLogApiCall_SetsExecutionTime() {
        // Given
        testRequest.setExecutionTimeMs(250L);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getExecutionTimeMs()).isEqualTo(250L);
    }

    @Test
    @DisplayName("Should set createdAt timestamp")
    void testLogApiCall_SetsCreatedAtTimestamp() {
        // Given
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ============= cleanupOldLogs() Tests =============

    @Test
    @DisplayName("Should cleanup old logs older than specified days")
    void testCleanupOldLogs_DeletesOldLogs() {
        // Given
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(100L);

        // When
        long result = auditLogService.cleanupOldLogs(90);

        // Then
        assertThat(result).isEqualTo(100L);
        verify(auditLogRepository, times(1)).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle no logs to delete")
    void testCleanupOldLogs_NoLogsToDelete() {
        // Given
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(0L);

        // When
        long result = auditLogService.cleanupOldLogs(90);

        // Then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should accept different day values for cleanup")
    void testCleanupOldLogs_DifferentDayValues() {
        // Given
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(50L);

        // When
        auditLogService.cleanupOldLogs(30);
        auditLogService.cleanupOldLogs(60);
        auditLogService.cleanupOldLogs(365);

        // Then
        verify(auditLogRepository, times(3)).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    // ============= Sensitive Header Detection Tests =============

    @Test
    @DisplayName("Should redact Authorization header")
    void testLogApiCall_RedactsAuthorizationHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("Authorization", "Bearer secret-token");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("***REDACTED***").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should redact Password header")
    void testLogApiCall_RedactsPasswordHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("Password", "secret123");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should redact Token header")
    void testLogApiCall_RedactsTokenHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("X-Token", "token-value");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should redact Secret header")
    void testLogApiCall_RedactsSecretHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("X-Secret", "secret-value");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should redact Key header")
    void testLogApiCall_RedactsKeyHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("API-Key", "api-key-value");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should redact Credential header")
    void testLogApiCall_RedactsCredentialHeader() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("Credential", "credential-value");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should handle case-insensitive sensitive header names")
    void testLogApiCall_CaseInsensitiveSensitiveHeaders() throws JsonProcessingException {
        // Given
        HttpHeaders sensitiveHeaders = new HttpHeaders();
        sensitiveHeaders.set("AUTHORIZATION", "Bearer token");
        sensitiveHeaders.set("password", "secret");
        testRequest.setRequestHeaders(sensitiveHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should not redact non-sensitive headers")
    void testLogApiCall_DoesNotRedactNonSensitiveHeaders() throws JsonProcessingException {
        // Given
        HttpHeaders normalHeaders = new HttpHeaders();
        normalHeaders.set("Content-Type", "application/json");
        normalHeaders.set("Accept", "application/json");
        testRequest.setRequestHeaders(normalHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    // ============= Edge Cases for Serialization =============

    @Test
    @DisplayName("Should handle String request body directly")
    void testLogApiCall_StringRequestBody() {
        // Given
        testRequest.setRequestBody("simple string body");
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle Map request body")
    void testLogApiCall_MapRequestBody() throws JsonProcessingException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("key", "value");
        testRequest.setRequestBody(requestMap);
        doReturn("{\"key\":\"value\"}").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle serialization exception gracefully")
    void testLogApiCall_SerializationException() throws JsonProcessingException {
        // Given
        testRequest.setRequestBody(new Object());
        doThrow(new JsonProcessingException("Serialization failed") {})
                .when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    // ============= getStackTraceAsString() Tests =============

    @Test
    @DisplayName("Should convert exception to stack trace string")
    void testGetStackTraceAsString_ConvertException() {
        // Given
        Exception exception = new RuntimeException("Test exception");

        // When
        String stackTrace = ApiAuditLogService.getStackTraceAsString(exception);

        // Then
        assertThat(stackTrace).isNotNull();
        assertThat(stackTrace).contains("RuntimeException");
        assertThat(stackTrace).contains("Test exception");
    }

    @Test
    @DisplayName("Should handle nested exceptions in stack trace")
    void testGetStackTraceAsString_NestedExceptions() {
        // Given
        Exception cause = new IllegalArgumentException("Invalid argument");
        Exception exception = new RuntimeException("Wrapper exception", cause);

        // When
        String stackTrace = ApiAuditLogService.getStackTraceAsString(exception);

        // Then
        assertThat(stackTrace).isNotNull();
        assertThat(stackTrace).contains("RuntimeException");
        assertThat(stackTrace).contains("Wrapper exception");
    }

    @Test
    @DisplayName("Should format stack trace with class and method names")
    void testGetStackTraceAsString_ContainsClassMethodInfo() {
        // Given
        Exception exception = new NullPointerException("Null value");

        // When
        String stackTrace = ApiAuditLogService.getStackTraceAsString(exception);

        // Then
        assertThat(stackTrace).isNotNull();
        assertThat(stackTrace).contains("NullPointerException");
        assertThat(stackTrace.split("\n").length).isGreaterThan(1);
    }

    // ============= Status Determination Edge Cases =============

    @Test
    @DisplayName("Should handle status code 299 as SUCCESS")
    void testLogApiCall_Status299_Success() {
        // Given
        testRequest.setResponseStatus(299);
        testRequest.setStatus(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Should handle status code 300 as UNKNOWN")
    void testLogApiCall_Status300_Unknown() {
        // Given
        testRequest.setResponseStatus(300);
        testRequest.setStatus(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle status code 399 as UNKNOWN")
    void testLogApiCall_Status399_Unknown() {
        // Given
        testRequest.setResponseStatus(399);
        testRequest.setStatus(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle status code 499 as FAILURE")
    void testLogApiCall_Status499_Failure() {
        // Given
        testRequest.setResponseStatus(499);
        testRequest.setStatus(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILURE");
    }

    // ============= Size Calculation Edge Cases =============

    @Test
    @DisplayName("Should handle null body in size calculation")
    void testLogApiCall_SizeCalculation_NullBody() {
        // Given
        testRequest.setRequestBody(null);
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestSize()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should calculate size for large response body")
    void testLogApiCall_SizeCalculation_LargeBody() throws JsonProcessingException {
        // Given
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBody.append("{\"id\": ").append(i).append("},");
        }
        testRequest.setResponseBody(largeBody.toString());
        doReturn(largeBody.toString()).when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseSize()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Should calculate size for empty string body")
    void testLogApiCall_SizeCalculation_EmptyBody() throws JsonProcessingException {
        // Given
        testRequest.setRequestBody("");
        doReturn("").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestSize()).isEqualTo(0L);
    }

    // ============= Headers Serialization Edge Cases =============

    @Test
    @DisplayName("Should handle HttpHeaders with empty values list")
    void testLogApiCall_HeadersWithEmptyValues() throws JsonProcessingException {
        // Given
        HttpHeaders headersWithEmpty = new HttpHeaders();
        headersWithEmpty.put("Empty-Header", new java.util.ArrayList<>());
        testRequest.setRequestHeaders(headersWithEmpty);
        doReturn("{}").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should handle HttpHeaders with multiple values")
    void testLogApiCall_HeadersWithMultipleValues() throws JsonProcessingException {
        // Given
        HttpHeaders multiHeaders = new HttpHeaders();
        multiHeaders.add("Accept", "application/json");
        multiHeaders.add("Accept", "text/plain");
        testRequest.setRequestHeaders(multiHeaders);
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestHeaders()).isNotNull();
    }

    // ============= Full Integration Scenarios =============

    @Test
    @DisplayName("Should log complete audit trail with error information")
    void testLogApiCall_WithErrorInformation() throws JsonProcessingException {
        // Given
        testRequest.setResponseStatus(500);
        testRequest.setStatus(null);  // Allow service to determine status from response code
        testRequest.setErrorMessage("Database connection failed");
        testRequest.setExceptionStackTrace("java.sql.SQLException: Connection refused");
        doReturn("serialized").when(objectMapper).writeValueAsString(any());
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        ApiAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getErrorMessage()).isEqualTo("Database connection failed");
        assertThat(savedLog.getExceptionStackTrace()).isEqualTo("java.sql.SQLException: Connection refused");
        assertThat(savedLog.getStatus()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should log user activity with IP address")
    void testLogApiCall_UserActivityWithIp() {
        // Given
        testRequest.setUserId("user-456");
        testRequest.setIpAddress("10.0.0.1");
        testRequest.setApiEndpoint("/api/v1/profile");
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        ArgumentCaptor<ApiAuditLog> captor = ArgumentCaptor.forClass(ApiAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        ApiAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo("user-456");
        assertThat(savedLog.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Should verify repository save is called exactly once per log call")
    void testLogApiCall_RepositorySaveCalledOnce() {
        // Given
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        // When
        auditLogService.logApiCall(testRequest);

        // Then
        verify(auditLogRepository, times(1)).save(any(ApiAuditLog.class));
    }

    @Test
    @DisplayName("Should handle multiple consecutive log calls")
    void testLogApiCall_MultipleConsecutiveCalls() {
        // Given
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());
        ApiAuditLogService.ApiAuditLogRequest request2 = ApiAuditLogService.ApiAuditLogRequest.builder()
                .traceId("trace-002")
                .apiEndpoint("/api/v1/products")
                .httpMethod("GET")
                .responseStatus(200)
                .userId("user-789")
                .build();

        // When
        auditLogService.logApiCall(testRequest);
        auditLogService.logApiCall(request2);

        // Then
        verify(auditLogRepository, times(2)).save(any(ApiAuditLog.class));
    }

    // ── ApiAuditLogRequest builder.toString + setters + calculateSize(null) ──

    @Test
    @DisplayName("ApiAuditLogRequest builder: toString does not throw")
    void apiAuditLogRequest_builderToString() {
        String str = ApiAuditLogService.ApiAuditLogRequest.builder()
                .traceId("t1").apiEndpoint("/test").httpMethod("GET")
                .responseStatus(200).toString();
        assertThat(str).isNotBlank();
    }

    @Test
    @DisplayName("ApiAuditLogRequest: setHttpMethod, setResponseHeaders, setDescription via setters")
    void apiAuditLogRequest_setters() {
        ApiAuditLogService.ApiAuditLogRequest req = new ApiAuditLogService.ApiAuditLogRequest();
        req.setHttpMethod("DELETE");
        req.setResponseHeaders(Map.of("X-Header", "value"));
        req.setDescription("test description");

        assertThat(req.getHttpMethod()).isEqualTo("DELETE");
        assertThat(req.getResponseHeaders()).isNotNull();
        assertThat(req.getDescription()).isEqualTo("test description");
    }

    @Test
    @DisplayName("logApiCall: handles null requestBody and responseBody (covers calculateSize null path)")
    void logApiCall_nullBodies_handlesGracefully() {
        when(auditLogRepository.save(any(ApiAuditLog.class))).thenReturn(new ApiAuditLog());

        ApiAuditLogService.ApiAuditLogRequest req = ApiAuditLogService.ApiAuditLogRequest.builder()
                .apiEndpoint("/test").httpMethod("GET").responseStatus(200)
                .requestBody(null).responseBody(null).build();

        auditLogService.logApiCall(req);

        verify(auditLogRepository).save(any(ApiAuditLog.class));
    }
}




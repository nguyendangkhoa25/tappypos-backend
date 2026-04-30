package com.knp.service.audit;

import com.knp.model.entity.audit.ApiAuditLog;
import com.knp.repository.audit.ApiAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for auditing API requests and responses
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApiAuditLogService {

    private final ApiAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an API request/response
     */
    public void logApiCall(ApiAuditLogRequest request) {
        try {
            String traceId = request.getTraceId() != null ? request.getTraceId() : UUID.randomUUID().toString();

            ApiAuditLog auditLog = ApiAuditLog.builder()
                    .traceId(traceId)
                    .apiEndpoint(request.getApiEndpoint())
                    .httpMethod(request.getHttpMethod())
                    .requestBody(safeSerializeToJson(request.getRequestBody()))
                    .requestHeaders(safeSerializeToJson(request.getRequestHeaders()))
                    .responseBody(safeSerializeToJson(request.getResponseBody()))
                    .responseHeaders(safeSerializeToJson(request.getResponseHeaders()))
                    .responseStatus(request.getResponseStatus())
                    .requestSize(calculateSize(request.getRequestBody()))
                    .responseSize(calculateSize(request.getResponseBody()))
                    .executionTimeMs(request.getExecutionTimeMs())
                    .errorMessage(request.getErrorMessage())
                    .exceptionStackTrace(request.getExceptionStackTrace())
                    .userId(request.getUserId())
                    .ipAddress(request.getIpAddress())
                    .status(request.getStatus() != null ? request.getStatus() : determineStatus(request.getResponseStatus()))
                    .createdAt(LocalDateTime.now())
                    .description(request.getDescription())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("API call logged with traceId: {}", traceId);
        } catch (Exception e) {
            log.error("Error while logging API audit", e);
        }
    }


    /**
     * Cleanup old logs (older than specified days)
     */
    public long cleanupOldLogs(int daysOld) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysOld);
        long deletedCount = auditLogRepository.deleteByCreatedAtBefore(beforeDate);
        log.info("Deleted {} old audit logs older than {} days", deletedCount, daysOld);
        return deletedCount;
    }

    /**
     * Safely serialize object to JSON string
     */
    private String safeSerializeToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            if (object instanceof String body) {
                return body;
            }
            if (object instanceof HttpHeaders headers) {
                return serializeHeaders(headers);
            }
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON", e);
            return object.toString();
        }
    }

    /**
     * Serialize HTTP headers safely (exclude sensitive headers)
     */
    private String serializeHeaders(HttpHeaders headers) {
        Map<String, String> safeHeaders = new HashMap<>();
        headers.forEach((key, values) -> {
            // Exclude sensitive headers
            if (!isSensitiveHeader(key) && !values.isEmpty()) {
                safeHeaders.put(key, values.get(0));
            } else if (isSensitiveHeader(key)) {
                safeHeaders.put(key, "***REDACTED***");
            }
        });
        try {
            return objectMapper.writeValueAsString(safeHeaders);
        } catch (Exception e) {
            log.warn("Failed to serialize headers", e);
            return "{}";
        }
    }

    /**
     * Check if header is sensitive
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerCase = headerName.toLowerCase();
        return lowerCase.contains("authorization") ||
                lowerCase.contains("password") ||
                lowerCase.contains("token") ||
                lowerCase.contains("secret") ||
                lowerCase.contains("key") ||
                lowerCase.contains("credential");
    }

    /**
     * Calculate size of an object
     */
    private Long calculateSize(Object object) {
        if (object == null) {
            return 0L;
        }
        try {
            String json = safeSerializeToJson(object);
            return (long) json.getBytes().length;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Determine status based on response status code
     */
    private String determineStatus(Integer responseStatus) {
        if (responseStatus == null) {
            return "UNKNOWN";
        }
        if (responseStatus >= 200 && responseStatus < 300) {
            return "SUCCESS";
        } else if (responseStatus >= 400 && responseStatus < 500) {
            return "FAILURE";
        } else if (responseStatus >= 500) {
            return "ERROR";
        }
        return "UNKNOWN";
    }

    /**
     * Convert exception to stack trace string
     */
    public static String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Request object for logging
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApiAuditLogRequest {
        private String traceId;
        private String apiEndpoint;
        private String httpMethod;
        private Object requestBody;
        private Object requestHeaders;
        private Object responseBody;
        private Object responseHeaders;
        private Integer responseStatus;
        private Long executionTimeMs;
        private String errorMessage;
        private String exceptionStackTrace;
        private String userId;
        private String ipAddress;
        private String status;
        private String description;
    }
}


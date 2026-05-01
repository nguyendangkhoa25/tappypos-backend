package com.knp.model.entity.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for logging API request/response as JSON for audit trail and debugging
 */
@Entity
@Table(name = "api_audit_log", indexes = {
    @Index(name = "idx_api_endpoint", columnList = "api_endpoint"),
    @Index(name = "idx_method", columnList = "http_method"),
    @Index(name = "idx_timestamp", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "response_status"),
    @Index(name = "idx_trace_id", columnList = "trace_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ApiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "trace_id", length = 100, nullable = false)
    private String traceId;

    @Column(name = "api_endpoint", length = 500, nullable = false)
    private String apiEndpoint;

    @Column(name = "http_method", length = 20, nullable = false)
    private String httpMethod;

    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    private String requestBody;

    @Column(name = "request_headers", columnDefinition = "LONGTEXT")
    private String requestHeaders;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "LONGTEXT")
    private String responseHeaders;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "exception_stack_trace", columnDefinition = "LONGTEXT")
    private String exceptionStackTrace;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILURE, PARTIAL_FAILURE

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "description")
    private String description;
}


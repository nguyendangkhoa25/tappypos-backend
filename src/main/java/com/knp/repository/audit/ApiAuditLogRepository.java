package com.knp.repository.audit;

import com.knp.model.entity.audit.ApiAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, Long> {

    Optional<ApiAuditLog> findByTraceId(String traceId);

    List<ApiAuditLog> findByApiEndpoint(String apiEndpoint);

    List<ApiAuditLog> findByHttpMethod(String httpMethod);

    Page<ApiAuditLog> findByStatus(String status, Pageable pageable);

    Page<ApiAuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT a FROM ApiAuditLog a WHERE a.apiEndpoint = :endpoint AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<ApiAuditLog> findByEndpointAndDateRange(
        @Param("endpoint") String endpoint,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT a FROM ApiAuditLog a WHERE a.status = :status AND a.responseStatus >= 400 AND a.createdAt >= :since")
    List<ApiAuditLog> findFailedRequestsSince(@Param("status") String status, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM ApiAuditLog a WHERE a.apiEndpoint = :endpoint AND a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    List<ApiAuditLog> findUserActivityLog(
        @Param("endpoint") String endpoint,
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long deleteByCreatedAtBefore(LocalDateTime beforeDate);
}


package com.knp.repository.audit;

import com.knp.model.entity.audit.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query(value = "SELECT * FROM activity_log WHERE " +
           "(:username IS NULL OR actor_username = :username) AND " +
           "(:action IS NULL OR action = :action) AND " +
           "(:targetType IS NULL OR target_type = :targetType) AND " +
           "(CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp)) AND " +
           "(CAST(:to AS timestamp) IS NULL OR created_at <= CAST(:to AS timestamp)) " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM activity_log WHERE " +
           "(:username IS NULL OR actor_username = :username) AND " +
           "(:action IS NULL OR action = :action) AND " +
           "(:targetType IS NULL OR target_type = :targetType) AND " +
           "(CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp)) AND " +
           "(CAST(:to AS timestamp) IS NULL OR created_at <= CAST(:to AS timestamp))",
           nativeQuery = true)
    Page<ActivityLog> findWithFilters(
            @Param("username") String username,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}

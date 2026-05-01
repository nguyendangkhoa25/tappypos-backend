package com.knp.repository.feedback;

import com.knp.model.entity.feedback.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<UserFeedback, Long> {

    @Query("SELECT f FROM UserFeedback f WHERE f.deleted = false AND f.tenantId = :tenantId AND f.username = :username ORDER BY f.createdAt DESC")
    Page<UserFeedback> findByTenantIdAndUsername(@Param("tenantId") String tenantId,
                                                  @Param("username") String username,
                                                  Pageable pageable);

    @Query(value = "SELECT * FROM user_feedback WHERE deleted = false " +
           "AND (CAST(:tenantId AS text) IS NULL OR tenant_id = CAST(:tenantId AS text)) " +
           "AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text)) " +
           "AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text)) " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM user_feedback WHERE deleted = false " +
           "AND (CAST(:tenantId AS text) IS NULL OR tenant_id = CAST(:tenantId AS text)) " +
           "AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text)) " +
           "AND (CAST(:type AS text) IS NULL OR type = CAST(:type AS text))",
           nativeQuery = true)
    Page<UserFeedback> findAll(@Param("tenantId") String tenantId,
                               @Param("status") String status,
                               @Param("type") String type,
                               Pageable pageable);
}

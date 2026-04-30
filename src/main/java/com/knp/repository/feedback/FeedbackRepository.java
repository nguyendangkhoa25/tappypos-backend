package com.knp.repository.feedback;

import com.knp.model.entity.feedback.UserFeedback;
import com.knp.model.enums.FeedbackStatus;
import com.knp.model.enums.FeedbackType;
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

    @Query("SELECT f FROM UserFeedback f WHERE f.deleted = false " +
           "AND (:tenantId IS NULL OR f.tenantId = :tenantId) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:type IS NULL OR f.type = :type) " +
           "ORDER BY f.createdAt DESC")
    Page<UserFeedback> findAll(@Param("tenantId") String tenantId,
                               @Param("status") FeedbackStatus status,
                               @Param("type") FeedbackType type,
                               Pageable pageable);
}

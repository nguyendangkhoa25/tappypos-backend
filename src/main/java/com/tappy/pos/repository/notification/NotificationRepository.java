package com.tappy.pos.repository.notification;

import com.tappy.pos.model.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * All queries use current_tenant_id() for explicit tenant scoping.
 * This is more reliable than @Filter for UPDATE queries and avoids
 * null-tenant notifications leaking into shop contexts.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT * FROM notifications WHERE user_id = :userId AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM notifications WHERE user_id = :userId AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    Page<Notification> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query(value = "SELECT * FROM notifications WHERE user_id = :userId AND type = :type AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM notifications WHERE user_id = :userId AND type = :type AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    Page<Notification> findByUserIdAndType(@Param("userId") String userId, @Param("type") String type, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = false AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    long countUnread(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = true, read_at = NOW() " +
           "WHERE user_id = :userId AND is_read = false AND deleted = false " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    int markAllRead(@Param("userId") String userId);
}

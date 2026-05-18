package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    /** Cross-tenant — kept for restart-recovery scan only; do not use in normal auth flows. */
    Optional<ActiveSession> findByUsername(String username);

    /** Tenant-scoped lookup. Pass null for master users (tenant_id IS NULL). */
    @Query(value = "SELECT * FROM active_sessions WHERE username = :username " +
                   "AND tenant_id IS NOT DISTINCT FROM CAST(:tenantId AS varchar) LIMIT 1",
           nativeQuery = true)
    Optional<ActiveSession> findByUsernameAndTenantId(@Param("username") String username,
                                                       @Param("tenantId") String tenantId);

    /** Tenant-scoped delete — only removes the session for this user in this specific tenant. */
    @Modifying
    @Query(value = "DELETE FROM active_sessions WHERE username = :username " +
                   "AND tenant_id IS NOT DISTINCT FROM CAST(:tenantId AS varchar)",
           nativeQuery = true)
    void deleteByUsernameAndTenantId(@Param("username") String username,
                                      @Param("tenantId") String tenantId);

    /** Cross-tenant delete — only for full user removal; do not call on normal logout. */
    void deleteByUsername(String username);
}

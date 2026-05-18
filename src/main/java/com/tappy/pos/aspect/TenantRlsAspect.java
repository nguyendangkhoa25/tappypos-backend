package com.tappy.pos.aspect;

import com.tappy.pos.multitenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Layer 2 + 3 of tenant isolation, activated at the start of every transaction.
 *
 * Runs at @Order(101), which is INSIDE the @Transactional advisor (@Order 100
 * from TransactionManagementConfig). This guarantees the EntityManager and JDBC
 * connection are already bound when this @Before fires.
 *
 * Layer 2 — Hibernate @Filter: adds WHERE tenant_id = :tenantId to every JPA query.
 * Layer 3 — PostgreSQL RLS:    SET LOCAL app.current_tenant = '<id>' on the
 *            connection so that RLS policies enforce isolation even for raw JDBC.
 *
 * Master context (tenantId == null): both layers are skipped; PostgreSQL RLS
 * sees an empty string from current_tenant_id() and returns only NULL-tenant rows.
 */
@Aspect
@Component
@Order(101)
@RequiredArgsConstructor
@Slf4j
public class TenantRlsAspect {

    private final TenantContext tenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Before(
        "@annotation(org.springframework.transaction.annotation.Transactional) || " +
        "@within(org.springframework.transaction.annotation.Transactional)"
    )
    public void activateTenantContext() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return;

        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        try {
            Session session = entityManager.unwrap(Session.class);

            // Layer 2: enable Hibernate filter (idempotent — skip if already armed)
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            }

            // Layer 3: set PostgreSQL session variable (is_local=true → resets on
            // commit/rollback, safe with HikariCP connection reuse)
            entityManager
                    .createNativeQuery("SELECT set_config('app.current_tenant', :id, true)")
                    .setParameter("id", tenantId)
                    .getSingleResult();

        } catch (Exception e) {
            log.warn("Failed to activate tenant filter/RLS for tenant '{}': {}",
                    tenantId, e.getMessage());
        }
    }
}

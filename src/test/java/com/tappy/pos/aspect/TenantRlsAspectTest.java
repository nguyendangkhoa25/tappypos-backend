package com.tappy.pos.aspect;

import com.tappy.pos.multitenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantRlsAspect Unit Tests")
class TenantRlsAspectTest {

    @Mock private TenantContext tenantContext;
    @Mock private EntityManager entityManager;
    @Mock private Session session;
    @Mock private Filter filter;

    @InjectMocks private TenantRlsAspect tenantRlsAspect;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantRlsAspect, "entityManager", entityManager);
    }

    @Test
    @DisplayName("activateTenantContext: skips when no active transaction")
    void activateTenantContext_noActiveTransaction() {
        try (MockedStatic<TransactionSynchronizationManager> tsm =
                mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);

            tenantRlsAspect.activateTenantContext();

            verify(tenantContext, never()).getCurrentTenantId();
        }
    }

    @Test
    @DisplayName("activateTenantContext: skips when tenantId is null (master context)")
    void activateTenantContext_nullTenantId() {
        try (MockedStatic<TransactionSynchronizationManager> tsm =
                mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            when(tenantContext.getCurrentTenantId()).thenReturn(null);

            tenantRlsAspect.activateTenantContext();

            verify(entityManager, never()).unwrap(any());
        }
    }

    @Test
    @DisplayName("activateTenantContext: enables Hibernate filter and sets RLS for tenant")
    void activateTenantContext_enablesFilterAndRls() {
        try (MockedStatic<TransactionSynchronizationManager> tsm =
                mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
            when(entityManager.unwrap(Session.class)).thenReturn(session);
            when(session.getEnabledFilter("tenantFilter")).thenReturn(null);
            when(session.enableFilter("tenantFilter")).thenReturn(filter);
            when(filter.setParameter("tenantId", "tenant1")).thenReturn(filter);

            var nativeQuery = mock(jakarta.persistence.Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq("id"), eq("tenant1"))).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(null);

            tenantRlsAspect.activateTenantContext();

            verify(session).enableFilter("tenantFilter");
            verify(filter).setParameter("tenantId", "tenant1");
            verify(entityManager).createNativeQuery(anyString());
        }
    }

    @Test
    @DisplayName("activateTenantContext: skips Hibernate filter if already enabled")
    void activateTenantContext_filterAlreadyEnabled() {
        try (MockedStatic<TransactionSynchronizationManager> tsm =
                mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            when(tenantContext.getCurrentTenantId()).thenReturn("tenant2");
            when(entityManager.unwrap(Session.class)).thenReturn(session);
            when(session.getEnabledFilter("tenantFilter")).thenReturn(filter);

            var nativeQuery = mock(jakarta.persistence.Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq("id"), eq("tenant2"))).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(null);

            tenantRlsAspect.activateTenantContext();

            verify(session, never()).enableFilter(anyString());
            verify(entityManager).createNativeQuery(anyString());
        }
    }

    @Test
    @DisplayName("activateTenantContext: logs warning and swallows exception on error")
    void activateTenantContext_exceptionIsSwallowed() {
        try (MockedStatic<TransactionSynchronizationManager> tsm =
                mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            when(tenantContext.getCurrentTenantId()).thenReturn("tenant3");
            when(entityManager.unwrap(Session.class)).thenThrow(new RuntimeException("DB error"));

            // Should not throw
            tenantRlsAspect.activateTenantContext();
        }
    }
}

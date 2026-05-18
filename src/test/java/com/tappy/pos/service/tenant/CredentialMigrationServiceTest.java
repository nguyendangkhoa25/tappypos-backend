package com.tappy.pos.service.tenant;

import com.tappy.pos.config.EncryptionService;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialMigrationService Unit Tests")
class CredentialMigrationServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;
    @Mock private EncryptionService encryptionService;
    @Mock private DataSource dataSource;

    @InjectMocks private CredentialMigrationService credentialMigrationService;

    private Tenant tenant1;
    private Tenant tenant2;

    @BeforeEach
    void setUp() {
        tenant1 = new Tenant();
        tenant1.setTenantId("shop001");

        tenant2 = new Tenant();
        tenant2.setTenantId("shop002");
    }

    @Test
    @DisplayName("encryptAllTenants: returns empty map when no active tenants")
    void encryptAllTenants_noTenants() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

        Map<String, Integer> results = credentialMigrationService.encryptAllTenants();

        assertThat(results).isEmpty();
        verify(tenantContext, never()).setCurrentTenant(any());
    }

    @Test
    @DisplayName("encryptAllTenants: sets and clears tenant context for each tenant")
    void encryptAllTenants_setsTenantContextForEach() {
        // DataSource mock returns null connection → JdbcTemplate throws → caught per-tenant
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(tenant1, tenant2));

        credentialMigrationService.encryptAllTenants();

        // setCurrentTenant is called for each tenant (NPE on JDBC is caught in try-catch)
        verify(tenantContext, times(2)).setCurrentTenant(any());
        // clear() is always called in finally
        verify(tenantContext, times(2)).clear();
    }

    @Test
    @DisplayName("encryptAllTenants: records -1 for tenant that throws exception")
    void encryptAllTenants_tenantException_recordsMinus1() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(tenant1));
        doThrow(new RuntimeException("DB error")).when(tenantContext).setCurrentTenant(tenant1);

        Map<String, Integer> results = credentialMigrationService.encryptAllTenants();

        assertThat(results.get("shop001")).isEqualTo(-1);
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("encryptAllTenants: processes multiple tenants independently")
    void encryptAllTenants_processesBothTenants() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(tenant1, tenant2));

        Map<String, Integer> results = credentialMigrationService.encryptAllTenants();

        assertThat(results).containsKey("shop001");
        assertThat(results).containsKey("shop002");
    }
}

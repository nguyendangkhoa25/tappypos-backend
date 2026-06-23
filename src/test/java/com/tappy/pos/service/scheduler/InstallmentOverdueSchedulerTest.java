package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.installment.InstallmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentOverdueScheduler Unit Tests")
class InstallmentOverdueSchedulerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;
    @Mock private InstallmentService installmentService;

    @InjectMocks
    private InstallmentOverdueScheduler scheduler;

    private Tenant tenant(String id, String features) {
        Tenant t = mock(Tenant.class);
        lenient().when(t.getFeatures()).thenReturn(features);
        lenient().when(t.getTenantId()).thenReturn(id);
        return t;
    }

    @Test
    @DisplayName("notifies only tenants that have the INSTALLMENT feature")
    void notifiesOnlyInstallmentTenants() {
        Tenant withFeature = tenant("shop-1", "ORDER,INSTALLMENT");
        Tenant withoutFeature = tenant("shop-2", "ORDER");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(withFeature, withoutFeature));

        scheduler.sendOverdueNotifications();

        verify(tenantContext).setCurrentTenant(withFeature);
        verify(tenantContext, never()).setCurrentTenant(withoutFeature);
        verify(installmentService, times(1)).notifyOverdue();
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("tenant with null features is skipped")
    void skipsTenantWithNullFeatures() {
        Tenant t = tenant("shop-1", null);
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(t));

        scheduler.sendOverdueNotifications();

        verify(installmentService, never()).notifyOverdue();
        verify(tenantContext, never()).clear();
    }

    @Test
    @DisplayName("empty tenant list does nothing")
    void emptyTenantList() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of());

        scheduler.sendOverdueNotifications();

        verify(installmentService, never()).notifyOverdue();
        verify(tenantContext, never()).setCurrentTenant(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("an exception for one tenant is isolated and context is still cleared")
    void perTenantExceptionIsolation() {
        Tenant bad = tenant("shop-1", "INSTALLMENT");
        Tenant good = tenant("shop-2", "INSTALLMENT");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(bad, good));
        doThrow(new RuntimeException("boom")).doNothing().when(installmentService).notifyOverdue();

        scheduler.sendOverdueNotifications();

        // both tenants processed despite the first failing
        verify(installmentService, times(2)).notifyOverdue();
        verify(tenantContext).setCurrentTenant(bad);
        verify(tenantContext).setCurrentTenant(good);
        verify(tenantContext, times(2)).clear();
    }
}

package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyRevenueSummaryScheduler Unit Tests")
class DailyRevenueSummarySchedulerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;
    @Mock private SchedulerNotificationHelper helper;

    @InjectMocks
    private DailyRevenueSummaryScheduler scheduler;

    private Tenant tenantWithFeatures(String features) {
        Tenant t = mock(Tenant.class);
        lenient().when(t.getFeatures()).thenReturn(features);
        lenient().when(t.getTenantId()).thenReturn("shop-1");
        return t;
    }

    @Test
    @DisplayName("processes only tenants that have the ORDER feature")
    void sendsForOrderFeatureTenants() {
        Tenant withOrder = tenantWithFeatures("ORDER,PAWN");
        Tenant withoutOrder = tenantWithFeatures("PAWN");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(withOrder, withoutOrder));

        scheduler.sendDailyRevenueSummaries();

        verify(helper).sendDailyRevenueSummary(withOrder);
        verify(helper, never()).sendDailyRevenueSummary(withoutOrder);
        verify(tenantContext).setCurrentTenant(withOrder);
    }

    @Test
    @DisplayName("a failure for one tenant does not abort the run")
    void continuesOnFailure() {
        Tenant t = tenantWithFeatures("ORDER");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(t));
        doThrow(new RuntimeException("boom")).when(helper).sendDailyRevenueSummary(t);

        scheduler.sendDailyRevenueSummaries();

        verify(tenantContext).clear();
    }
}

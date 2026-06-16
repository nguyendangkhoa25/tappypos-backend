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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PawnDueNotificationScheduler Unit Tests")
class PawnDueNotificationSchedulerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;
    @Mock private SchedulerNotificationHelper helper;

    @InjectMocks
    private PawnDueNotificationScheduler scheduler;

    private Tenant tenantWithFeatures(String features) {
        Tenant t = mock(Tenant.class);
        lenient().when(t.getFeatures()).thenReturn(features);
        lenient().when(t.getTenantId()).thenReturn("shop-1");
        return t;
    }

    @Test
    @DisplayName("notifies only tenants that have the PAWN feature")
    void sendsForPawnFeatureTenants() {
        Tenant withPawn = tenantWithFeatures("ORDER,PAWN");
        Tenant withoutPawn = tenantWithFeatures("ORDER");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(withPawn, withoutPawn));

        scheduler.sendPawnDueNotifications();

        verify(helper).sendPawnDueNotification(withPawn);
        verify(helper, never()).sendPawnDueNotification(withoutPawn);
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("tenant with no features is skipped")
    void skipsTenantWithNoFeatures() {
        Tenant t = tenantWithFeatures(null);
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(t));

        scheduler.sendPawnDueNotifications();

        verify(helper, never()).sendPawnDueNotification(t);
    }
}

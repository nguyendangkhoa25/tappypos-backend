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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentReminderScheduler Unit Tests")
class AppointmentReminderSchedulerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;
    @Mock private SchedulerNotificationHelper helper;

    @InjectMocks
    private AppointmentReminderScheduler scheduler;

    private Tenant tenantWithFeatures(String features) {
        Tenant t = mock(Tenant.class);
        lenient().when(t.getFeatures()).thenReturn(features);
        lenient().when(t.getTenantId()).thenReturn("shop-1");
        return t;
    }

    @Test
    @DisplayName("sends reminders only for tenants with the APPOINTMENT feature")
    void sendsForAppointmentFeatureTenants() {
        Tenant withAppt = tenantWithFeatures("APPOINTMENT");
        Tenant withoutAppt = tenantWithFeatures("ORDER");
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(withAppt, withoutAppt));

        scheduler.sendReminders();

        verify(helper).sendAppointmentReminders(eq(withAppt), any(LocalTime.class));
        verify(helper, never()).sendAppointmentReminders(eq(withoutAppt), any());
    }

    @Test
    @DisplayName("no active tenants → helper never invoked")
    void noTenants() {
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of());

        scheduler.sendReminders();

        verify(helper, never()).sendAppointmentReminders(any(), any());
    }
}

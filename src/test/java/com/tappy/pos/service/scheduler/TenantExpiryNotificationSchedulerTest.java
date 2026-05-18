package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantExpiryNotificationScheduler Unit Tests")
class TenantExpiryNotificationSchedulerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private NotificationService notificationService;
    @Mock private TenantContext tenantContext;

    @InjectMocks
    private TenantExpiryNotificationScheduler scheduler;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .tenantId("shop1")
                .name("Test Shop")
                .active(true)
                .subscriptionType("STANDARD")
                .build();
        tenant.setId(1L);
        // Default: no tenants expiring (override per test)
        lenient().when(tenantRepository.findActiveTenantsExpiringOn(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("sendExpiryWarningNotifications: pushes warning for each expiring tenant on each warn day")
    void sendExpiryWarningNotifications_tenantExpiring() {
        LocalDate in7 = LocalDate.now().plusDays(7);
        when(tenantRepository.findActiveTenantsExpiringOn(in7)).thenReturn(List.of(tenant));

        scheduler.sendExpiryWarningNotifications();

        verify(tenantContext).setCurrentTenant(tenant);
        verify(notificationService).pushExpiryWarning(tenant, 7);
        verify(tenantContext, atLeastOnce()).clear();
    }

    @Test
    @DisplayName("sendExpiryWarningNotifications: skips notification when no tenants expiring")
    void sendExpiryWarningNotifications_noExpiring() {
        scheduler.sendExpiryWarningNotifications();

        verify(notificationService, never()).pushExpiryWarning(any(), anyInt());
    }

    @Test
    @DisplayName("sendExpiryWarningNotifications: clears context even when pushExpiryWarning throws")
    void sendExpiryWarningNotifications_exceptionHandled() {
        LocalDate in3 = LocalDate.now().plusDays(3);
        when(tenantRepository.findActiveTenantsExpiringOn(in3)).thenReturn(List.of(tenant));
        doThrow(new RuntimeException("push failed")).when(notificationService).pushExpiryWarning(tenant, 3);

        scheduler.sendExpiryWarningNotifications();

        verify(tenantContext, atLeastOnce()).clear();
    }

    @Test
    @DisplayName("sendExpiryWarningNotifications: notifies multiple tenants for the same warn day")
    void sendExpiryWarningNotifications_multipleTenants() {
        Tenant t2 = Tenant.builder().tenantId("shop2").name("Shop 2").active(true)
                .subscriptionType("STANDARD").build();
        t2.setId(2L);

        LocalDate in1 = LocalDate.now().plusDays(1);
        when(tenantRepository.findActiveTenantsExpiringOn(in1)).thenReturn(List.of(tenant, t2));

        scheduler.sendExpiryWarningNotifications();

        verify(notificationService).pushExpiryWarning(tenant, 1);
        verify(notificationService).pushExpiryWarning(t2, 1);
    }
}

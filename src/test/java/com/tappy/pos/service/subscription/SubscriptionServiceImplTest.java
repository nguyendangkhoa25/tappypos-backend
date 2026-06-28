package com.tappy.pos.service.subscription;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl Unit Tests")
class SubscriptionServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private TenantRepository tenantRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl service;

    private Tenant tenant(String type, boolean active, LocalDate expiration) {
        Tenant t = mock(Tenant.class);
        lenient().when(t.getSubscriptionType()).thenReturn(type);
        lenient().when(t.isActive()).thenReturn(active);
        lenient().when(t.getExpirationDate()).thenReturn(expiration);
        lenient().when(t.getMaxUsers()).thenReturn(2);
        return t;
    }

    @Test
    @DisplayName("getForCurrentTenant: maps plan limits and real usage")
    void getForCurrentTenant() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        Tenant t = tenant("BASIC", true, LocalDate.now().plusMonths(6));
        when(tenantRepository.findByTenantId("shop-1")).thenReturn(Optional.of(t));
        when(orderRepository.countOrdersThisMonth(anyInt(), anyInt())).thenReturn(12L);
        when(userRepository.countByTenantId("shop-1")).thenReturn(2);

        Map<String, Object> data = service.getForCurrentTenant();

        assertThat(data.get("plan")).isEqualTo("BASIC");
        assertThat(data.get("status")).isEqualTo("ACTIVE");
        assertThat(data.get("maxUsers")).isEqualTo(2);          // Cơ bản = 2 users
        assertThat(data.get("currentUsers")).isEqualTo(2);      // real count, not the limit
        assertThat(data.get("maxOrdersPerMonth")).isNull();     // orders unlimited on every plan
        assertThat(data.get("currentMonthOrders")).isEqualTo(12L);
    }

    @Test
    @DisplayName("getForCurrentTenant: inactive tenant → SUSPENDED")
    void getForCurrentTenant_suspended() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        Tenant t = tenant("BASIC", false, LocalDate.now().plusDays(5));
        when(tenantRepository.findByTenantId("shop-1")).thenReturn(Optional.of(t));
        when(orderRepository.countOrdersThisMonth(anyInt(), anyInt())).thenReturn(0L);
        when(userRepository.countByTenantId("shop-1")).thenReturn(1);

        assertThat(service.getForCurrentTenant().get("status")).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("getForCurrentTenant: unknown plan code degrades to default BASIC (no 500 on the read path)")
    void getForCurrentTenant_unknownPlan() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        Tenant t = tenant("LEGACY_BOGUS", true, LocalDate.now().plusMonths(6));
        when(tenantRepository.findByTenantId("shop-1")).thenReturn(Optional.of(t));
        when(orderRepository.countOrdersThisMonth(anyInt(), anyInt())).thenReturn(0L);
        when(userRepository.countByTenantId("shop-1")).thenReturn(1);

        // Resilient: bad/legacy code falls back to the default plan (BASIC) limits instead of throwing.
        Map<String, Object> data = service.getForCurrentTenant();
        assertThat(data.get("maxUsers")).isEqualTo(2);       // BASIC = 2 users
        assertThat(data.get("maxOrdersPerMonth")).isNull();  // orders unlimited on every plan
    }

    @Test
    @DisplayName("checkOrderLimit: current plans are unlimited → no-op")
    void checkOrderLimit_unlimited() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        Tenant t = tenant("PRO", true, LocalDate.now().plusYears(1));
        when(tenantRepository.findByTenantId("shop-1")).thenReturn(Optional.of(t));

        assertThatCode(() -> service.checkOrderLimit()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkOrderLimit: missing tenant is a no-op")
    void checkOrderLimit_noTenant() {
        when(tenantContext.getCurrentTenantId()).thenReturn("ghost");
        when(tenantRepository.findByTenantId("ghost")).thenReturn(Optional.empty());

        assertThatCode(() -> service.checkOrderLimit()).doesNotThrowAnyException();
    }
}

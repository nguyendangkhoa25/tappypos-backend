package com.knp.service.audit;

import com.knp.model.entity.audit.ActivityLog;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.enums.ActivityAction;
import com.knp.multitenant.TenantContext;
import com.knp.repository.audit.ActivityLogRepository;
import com.knp.repository.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityLogServiceImpl Unit Tests")
class ActivityLogServiceImplTest {

    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContext tenantContext;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantId("shop1");
    }

    // ── logAsync ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logAsync: saves activity log entry with correct fields")
    void logAsync_success() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("shop1", "user1", "User One",
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "42",
                "Created product iPhone 15", "192.168.1.1");

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getActorUsername()).isEqualTo("user1");
        assertThat(cap.getValue().getAction()).isEqualTo(ActivityAction.PRODUCT_CREATED.name());
        assertThat(cap.getValue().getTargetType()).isEqualTo("PRODUCT");
        assertThat(cap.getValue().getTargetId()).isEqualTo("42");
        assertThat(cap.getValue().getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("logAsync: clears tenant context in finally block")
    void logAsync_clearsTenantContext() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenReturn(new ActivityLog());

        activityLogService.logAsync("shop1", "user1", null,
                ActivityAction.ORDER_CANCELLED, "ORDER", "1", "Deleted", null);

        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("logAsync: skips when tenantId is null")
    void logAsync_nullTenantId() {
        activityLogService.logAsync(null, "user1", null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "desc", null);

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAsync: skips when actorUsername is null")
    void logAsync_nullActor() {
        activityLogService.logAsync("shop1", null, null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "desc", null);

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAsync: skips when tenant not found in DB")
    void logAsync_tenantNotFound() {
        when(tenantRepository.findByTenantId("unknown")).thenReturn(Optional.empty());

        activityLogService.logAsync("unknown", "user1", null,
                ActivityAction.PRODUCT_UPDATED, "PRODUCT", "1", "desc", null);

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logAsync: clears tenant context even when save throws")
    void logAsync_clearsTenantContextOnException() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        activityLogService.logAsync("shop1", "user1", null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "desc", null);

        verify(tenantContext).clear();
    }

    // ── getActivityLogs ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getActivityLogs: normalizes blank filter strings to null")
    void getActivityLogs_blankFiltersNormalizedToNull() {
        Page<ActivityLog> emptyPage = new PageImpl<>(List.of());
        when(activityLogRepository.findWithFilters(isNull(), isNull(), isNull(), any(), any(), any()))
                .thenReturn(emptyPage);

        activityLogService.getActivityLogs("  ", "  ", "  ", null, null,
                PageRequest.of(0, 10));

        verify(activityLogRepository).findWithFilters(isNull(), isNull(), isNull(),
                isNull(), isNull(), any());
    }

    @Test
    @DisplayName("getActivityLogs: passes non-blank filters through")
    void getActivityLogs_withFilters() {
        Page<ActivityLog> emptyPage = new PageImpl<>(List.of());
        when(activityLogRepository.findWithFilters(eq("user1"), eq("PRODUCT_CREATED"), eq("PRODUCT"),
                any(), any(), any())).thenReturn(emptyPage);

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        activityLogService.getActivityLogs("user1", "PRODUCT_CREATED", "PRODUCT", from, to,
                PageRequest.of(0, 10));

        verify(activityLogRepository).findWithFilters(eq("user1"), eq("PRODUCT_CREATED"), eq("PRODUCT"),
                eq(from), eq(to), any());
    }
}

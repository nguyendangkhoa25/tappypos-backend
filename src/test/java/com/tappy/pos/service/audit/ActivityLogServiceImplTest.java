package com.tappy.pos.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.entity.audit.ActivityLog;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.audit.ActivityLogRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Mock private MessageService messageService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

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

    // ── logAsync: master/actor handling & arg serialization ─────────────────────

    @Test
    @DisplayName("logAsync: master tenantId is translated to null tenant_id and skips tenant lookup")
    void logAsync_masterTenantTranslatedToNull() {
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("master", "admin", "Admin",
                ActivityAction.TENANT_CREATED, "TENANT", "9",
                "activity.tenant.created", "10.0.0.1", "Shop X");

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isNull();
        assertThat(cap.getValue().getDescriptionKey()).isEqualTo("activity.tenant.created");
        // master path must not consult tenant repo or set tenant context
        verify(tenantRepository, never()).findByTenantId(anyString());
        verify(tenantContext, never()).setCurrentTenant(any());
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("logAsync: case-insensitive MASTER is treated as master")
    void logAsync_masterCaseInsensitive() {
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("MASTER", "admin", null,
                ActivityAction.AGENT_CREATED, "AGENT", "1", "activity.agent.created", null);

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isNull();
        verify(tenantRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("logAsync: shop tenantId is stored and tenant context is set")
    void logAsync_shopTenantStored() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("shop1", "user1", "User One",
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "activity.product.created", null);

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo("shop1");
        verify(tenantContext).setCurrentTenant(tenant);
    }

    @Test
    @DisplayName("logAsync: skips when tenantId is blank")
    void logAsync_blankTenantId() {
        activityLogService.logAsync("   ", "user1", null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "desc", null);

        verify(activityLogRepository, never()).save(any());
        verify(tenantContext, never()).clear();
    }

    @Test
    @DisplayName("logAsync: serializes varargs into descriptionArgs JSON")
    void logAsync_serializesArgs() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("shop1", "user1", null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1",
                "activity.product.created", null, "Áo thun", "1.500.000 ₫");

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getDescriptionArgs())
                .contains("Áo thun")
                .contains("1.500.000");
    }

    @Test
    @DisplayName("logAsync: stores null descriptionArgs when no varargs given")
    void logAsync_noArgsNullDescriptionArgs() {
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(activityLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        activityLogService.logAsync("shop1", "user1", null,
                ActivityAction.PRODUCT_CREATED, "PRODUCT", "1", "activity.product.created", null);

        ArgumentCaptor<ActivityLog> cap = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(cap.capture());
        assertThat(cap.getValue().getDescriptionArgs()).isNull();
    }

    // ── toDto / renderDescription (via getActivityLogs) ─────────────────────────

    @Test
    @DisplayName("getActivityLogs: renders description from i18n key in reader locale")
    void getActivityLogs_rendersFromKey() {
        ActivityLog entry = ActivityLog.builder()
                .id(7L)
                .actorUsername("user1")
                .actorFullName("User One")
                .action(ActivityAction.PRODUCT_CREATED.name())
                .targetType("PRODUCT")
                .targetId("1")
                .descriptionKey("activity.product.created")
                .descriptionArgs("[\"Áo thun\"]")
                .ipAddress("10.0.0.1")
                .build();
        when(messageService.getMessage(eq("activity.product.created"), any(Object[].class)))
                .thenReturn("Đã tạo sản phẩm Áo thun");
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        ActivityLogDTO dto = result.getContent().getFirst();
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getActorUsername()).isEqualTo("user1");
        assertThat(dto.getAction()).isEqualTo("PRODUCT_CREATED");
        assertThat(dto.getDescription()).isEqualTo("Đã tạo sản phẩm Áo thun");
    }

    @Test
    @DisplayName("getActivityLogs: falls back to legacy description when key is null")
    void getActivityLogs_fallbackToLegacyDescription() {
        ActivityLog entry = ActivityLog.builder()
                .id(8L)
                .actorUsername("user1")
                .action(ActivityAction.PRODUCT_UPDATED.name())
                .description("Đã cập nhật sản phẩm (legacy)")
                .descriptionKey(null)
                .build();
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().getDescription())
                .isEqualTo("Đã cập nhật sản phẩm (legacy)");
        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("getActivityLogs: blank key falls back to legacy description")
    void getActivityLogs_blankKeyFallback() {
        ActivityLog entry = ActivityLog.builder()
                .id(9L)
                .actorUsername("user1")
                .action(ActivityAction.PRODUCT_UPDATED.name())
                .description("legacy text")
                .descriptionKey("   ")
                .build();
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("legacy text");
    }

    @Test
    @DisplayName("getActivityLogs: when key cannot be resolved, falls back to legacy description")
    void getActivityLogs_renderFailureFallsBackToDescription() {
        ActivityLog entry = ActivityLog.builder()
                .id(10L)
                .actorUsername("user1")
                .action(ActivityAction.PRODUCT_CREATED.name())
                .description("frozen fallback")
                .descriptionKey("activity.unknown.key")
                .descriptionArgs(null)
                .build();
        when(messageService.getMessage(eq("activity.unknown.key"), any(Object[].class)))
                .thenThrow(new RuntimeException("no such message"));
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("frozen fallback");
    }

    @Test
    @DisplayName("getActivityLogs: render failure with null legacy description falls back to the key itself")
    void getActivityLogs_renderFailureNullDescriptionFallsBackToKey() {
        ActivityLog entry = ActivityLog.builder()
                .id(11L)
                .actorUsername("user1")
                .action(ActivityAction.PRODUCT_CREATED.name())
                .description(null)
                .descriptionKey("activity.broken.key")
                .build();
        when(messageService.getMessage(eq("activity.broken.key"), any(Object[].class)))
                .thenThrow(new RuntimeException("boom"));
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("activity.broken.key");
    }

    @Test
    @DisplayName("getActivityLogs: malformed descriptionArgs JSON still renders with empty args")
    void getActivityLogs_malformedArgsDeserializesToEmpty() {
        ActivityLog entry = ActivityLog.builder()
                .id(12L)
                .actorUsername("user1")
                .action(ActivityAction.PRODUCT_CREATED.name())
                .descriptionKey("activity.product.created")
                .descriptionArgs("{ not valid json")
                .build();
        ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
        when(messageService.getMessage(eq("activity.product.created"), argsCap.capture()))
                .thenReturn("rendered");
        when(activityLogRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent().getFirst().getDescription()).isEqualTo("rendered");
        // bad JSON deserializes to the NO_ARGS empty array
        assertThat(argsCap.getValue()).isEmpty();
    }
}

package com.tappy.pos.service.feedback;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.feedback.CreateFeedbackRequest;
import com.tappy.pos.model.dto.feedback.FeedbackDTO;
import com.tappy.pos.model.dto.feedback.UpdateFeedbackRequest;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.entity.feedback.UserFeedback;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.feedback.FeedbackRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.service.notification.NotificationService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl Unit Tests")
class FeedbackServiceImplTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private NotificationService notificationService;
    @Mock private AgentRepository agentRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.tappy.pos.service.audit.ActivityLogService activityLogService;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    @BeforeEach
    void setUp() {
        lenient().when(authContext.getCurrentUsername()).thenReturn("user1");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves feedback with correct tenant and username")
    void create_success() {
        CreateFeedbackRequest req = new CreateFeedbackRequest();
        req.setType(FeedbackType.BUG_REPORT);
        req.setTitle("Button broken");
        req.setContent("The save button doesn't work");

        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FeedbackDTO dto = feedbackService.create(req);

        ArgumentCaptor<UserFeedback> cap = ArgumentCaptor.forClass(UserFeedback.class);
        verify(feedbackRepository).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo("shop1");
        assertThat(cap.getValue().getUsername()).isEqualTo("user1");
        assertThat(cap.getValue().getStatus()).isEqualTo(FeedbackStatus.PENDING);
        // verify tenant context was cleared to force master DB
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("create: uses 'master' when no tenant context")
    void create_noTenant_usesMaster() {
        when(tenantContext.getCurrentTenantId()).thenReturn(null);

        CreateFeedbackRequest req = new CreateFeedbackRequest();
        req.setType(FeedbackType.SUGGESTION);
        req.setTitle("Feature idea");
        req.setContent("Please add dark mode");

        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        feedbackService.create(req);

        ArgumentCaptor<UserFeedback> cap = ArgumentCaptor.forClass(UserFeedback.class);
        verify(feedbackRepository).save(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo("master");
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: updates status and admin note")
    void updateStatus_success() {
        UserFeedback feedback = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.BUG_REPORT).title("Bug")
                .content("desc").status(FeedbackStatus.PENDING)
                .build();

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateFeedbackRequest req = new UpdateFeedbackRequest();
        req.setStatus(FeedbackStatus.IN_REVIEW);
        req.setAdminNote("Working on it");

        FeedbackDTO dto = feedbackService.updateStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo(FeedbackStatus.IN_REVIEW);
        assertThat(dto.getAdminNote()).isEqualTo("Working on it");
        assertThat(dto.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("updateStatus: sets resolvedAt when status is RESOLVED")
    void updateStatus_resolved() {
        UserFeedback feedback = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.BUG_REPORT).title("Bug")
                .content("desc").status(FeedbackStatus.PENDING)
                .build();

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateFeedbackRequest req = new UpdateFeedbackRequest();
        req.setStatus(FeedbackStatus.RESOLVED);

        FeedbackDTO dto = feedbackService.updateStatus(1L, req);

        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: throws when feedback not found")
    void updateStatus_notFound() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.updateStatus(99L, new UpdateFeedbackRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMyFeedback ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyFeedback: returns feedback for current tenant and user")
    void getMyFeedback_success() {
        UserFeedback feedback = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.BUG_REPORT).title("Bug").content("desc")
                .status(FeedbackStatus.PENDING).build();

        when(feedbackRepository.findByTenantIdAndUsername("shop1", "user1", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(feedback)));

        Page<FeedbackDTO> result = feedbackService.getMyFeedback(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTenantId()).isEqualTo("shop1");
    }

    @Test
    @DisplayName("getMyFeedback: uses 'master' tenantId when context has no tenant")
    void getMyFeedback_noTenant() {
        when(tenantContext.getCurrentTenantId()).thenReturn(null);
        when(feedbackRepository.findByTenantIdAndUsername(eq("master"), eq("user1"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FeedbackDTO> result = feedbackService.getMyFeedback(PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        verify(feedbackRepository).findByTenantIdAndUsername(eq("master"), eq("user1"), any());
    }

    // ── getAllFeedback ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllFeedback: returns all feedback without filters")
    void getAllFeedback_noFilters() {
        UserFeedback feedback = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.SUGGESTION).title("Idea").content("desc")
                .status(FeedbackStatus.PENDING).build();

        when(feedbackRepository.findAll(isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(feedback)));

        Page<FeedbackDTO> result = feedbackService.getAllFeedback(null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getAllFeedback: passes status and type names to repository")
    void getAllFeedback_withFilters() {
        when(feedbackRepository.findAll(eq("shop1"), eq("PENDING"), eq("BUG_REPORT"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FeedbackDTO> result = feedbackService.getAllFeedback(
                "shop1", FeedbackStatus.PENDING, FeedbackType.BUG_REPORT, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        verify(feedbackRepository).findAll("shop1", "PENDING", "BUG_REPORT", PageRequest.of(0, 20));
    }

    // ── SUBSCRIPTION_REQUEST notification ────────────────────────────────────

    @Test
    @DisplayName("create: triggers notification for SUBSCRIPTION_REQUEST type")
    void create_subscriptionRequest_notifies() {
        CreateFeedbackRequest req = new CreateFeedbackRequest();
        req.setType(FeedbackType.SUBSCRIPTION_REQUEST);
        req.setTitle("Upgrade plan");
        req.setContent("Want premium");

        UserFeedback saved = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.SUBSCRIPTION_REQUEST).title("Upgrade plan").content("Want premium")
                .status(FeedbackStatus.PENDING).build();
        saved.setId(5L);

        when(feedbackRepository.save(any())).thenReturn(saved);

        feedbackService.create(req);

        verify(notificationService).pushToMasterUsers(anyString(), anyString(), eq("FEEDBACK"), eq(5L));
    }

    @Test
    @DisplayName("create: notifies agent user when subscription request has vendorId")
    void create_subscriptionRequest_notifiesAgent() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setName("Shop A");
        tenant.setVendorId(10L);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        Agent agent = new Agent();
        agent.setId(10L);
        agent.setUserId(20L);
        when(agentRepository.findById(10L)).thenReturn(Optional.of(agent));

        User user = new User();
        user.setUsername("agent-user");
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        CreateFeedbackRequest req = new CreateFeedbackRequest();
        req.setType(FeedbackType.SUBSCRIPTION_REQUEST);
        req.setTitle("Upgrade");
        req.setContent("Need more features");

        UserFeedback saved = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.SUBSCRIPTION_REQUEST).title("Upgrade").content("Need more features")
                .status(FeedbackStatus.PENDING).build();
        saved.setId(7L);
        when(feedbackRepository.save(any())).thenReturn(saved);

        feedbackService.create(req);

        verify(notificationService).pushToMasterUsers(anyString(), anyString(), eq("FEEDBACK"), eq(7L));
        verify(notificationService).pushSystem(
                eq("agent-user"), eq(Notification.NotificationType.SYSTEM),
                anyString(), anyString(), eq("FEEDBACK"), eq(7L));
    }

    @Test
    @DisplayName("create: silently skips agent notification when agent userId is null")
    void create_subscriptionRequest_agentNoUserId() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setName("Shop A");
        tenant.setVendorId(10L);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        Agent agent = new Agent();
        agent.setId(10L);
        agent.setUserId(null);
        when(agentRepository.findById(10L)).thenReturn(Optional.of(agent));

        CreateFeedbackRequest req = new CreateFeedbackRequest();
        req.setType(FeedbackType.SUBSCRIPTION_REQUEST);
        req.setTitle("Upgrade");
        req.setContent("Need more features");

        UserFeedback saved = UserFeedback.builder()
                .tenantId("shop1").username("user1")
                .type(FeedbackType.SUBSCRIPTION_REQUEST).title("Upgrade").content("Need more features")
                .status(FeedbackStatus.PENDING).build();
        saved.setId(8L);
        when(feedbackRepository.save(any())).thenReturn(saved);

        feedbackService.create(req);

        verify(notificationService, never()).pushSystem(anyString(), any(), anyString(), anyString(), anyString(), any());
    }
}

package com.knp.service.feedback;

import com.knp.config.AuthContext;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.feedback.CreateFeedbackRequest;
import com.knp.model.dto.feedback.FeedbackDTO;
import com.knp.model.dto.feedback.UpdateFeedbackRequest;
import com.knp.model.entity.feedback.UserFeedback;
import com.knp.model.enums.FeedbackStatus;
import com.knp.model.enums.FeedbackType;
import com.knp.multitenant.TenantContext;
import com.knp.repository.feedback.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl Unit Tests")
class FeedbackServiceImplTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;

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
}

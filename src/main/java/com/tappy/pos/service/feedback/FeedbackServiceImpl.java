package com.tappy.pos.service.feedback;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.dto.feedback.CreateFeedbackRequest;
import com.tappy.pos.model.dto.feedback.FeedbackDTO;
import com.tappy.pos.model.dto.feedback.UpdateFeedbackRequest;
import com.tappy.pos.model.entity.feedback.UserFeedback;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.feedback.FeedbackRepository;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final NotificationService notificationService;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public FeedbackDTO create(CreateFeedbackRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        String tenantName = tenantContext.getCurrentTenant() != null
                ? tenantContext.getCurrentTenant().getName() : tenantId;
        Long agentId = tenantContext.getCurrentTenant() != null
                ? tenantContext.getCurrentTenant().getVendorId() : null;
        String username = authContext.getCurrentUsername();
        tenantContext.clear(); // force master DB

        UserFeedback feedback = UserFeedback.builder()
                .tenantId(tenantId != null ? tenantId : "master")
                .username(username)
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .status(FeedbackStatus.PENDING)
                .build();

        UserFeedback saved = feedbackRepository.save(feedback);

        if (request.getType() == FeedbackType.SUBSCRIPTION_REQUEST) {
            notifySubscriptionRequest(tenantName, saved.getId(), agentId);
        }

        return toDTO(saved);
    }

    @Override
    public Page<FeedbackDTO> getMyFeedback(Pageable pageable) {
        String tenantId = tenantContext.getCurrentTenantId();
        String username = authContext.getCurrentUsername();
        tenantContext.clear(); // force master DB
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return feedbackRepository
                .findByTenantIdAndUsername(tenantId != null ? tenantId : "master", username, unsorted)
                .map(this::toDTO);
    }

    @Override
    public Page<FeedbackDTO> getAllFeedback(String tenantId, FeedbackStatus status, FeedbackType type, Pageable pageable) {
        tenantContext.clear(); // force master DB
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return feedbackRepository.findAll(tenantId,
                status != null ? status.name() : null,
                type != null ? type.name() : null,
                unsorted).map(this::toDTO);
    }

    @Override
    @Transactional
    public FeedbackDTO updateStatus(Long id, UpdateFeedbackRequest request) {
        tenantContext.clear(); // force master DB
        UserFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));

        feedback.setStatus(request.getStatus());
        feedback.setAdminNote(request.getAdminNote());
        if (request.getStatus() == FeedbackStatus.RESOLVED || request.getStatus() == FeedbackStatus.CLOSED) {
            feedback.setResolvedAt(LocalDateTime.now());
        }
        UserFeedback saved = feedbackRepository.save(feedback);
        String reviewer = authContext.getCurrentUsername();
        activityLogService.logAsync("master", reviewer, null,
                ActivityAction.FEEDBACK_REVIEWED, "FEEDBACK", String.valueOf(id),
                "Xử lý phản hồi #" + id + " → " + request.getStatus().getDisplayName(), null);
        return toDTO(saved);
    }

    // After tenantContext.clear() — we are in master context; notifications are saved with tenant_id=null
    // (UnifiedTenantEntity master records) so master/agent users see them regardless of context.
    private void notifySubscriptionRequest(String shopName, Long feedbackId, Long agentId) {
        String title = "[Yêu cầu gói] " + shopName;
        String message = "Cửa hàng \"" + shopName + "\" vừa gửi yêu cầu thay đổi gói dịch vụ. Xem chi tiết trong mục Phản hồi.";

        try {
            notificationService.pushToMasterUsers(title, message, "FEEDBACK", feedbackId);
        } catch (Exception e) {
            log.warn("Failed to notify master users for subscription request {}: {}", feedbackId, e.getMessage());
        }

        if (agentId != null) {
            try {
                agentRepository.findById(agentId).ifPresent((Agent agent) -> {
                    if (agent.getUserId() != null) {
                        userRepository.findById(agent.getUserId()).ifPresent(user ->
                            notificationService.pushSystem(
                                    user.getUsername(), Notification.NotificationType.SYSTEM, title, message, "FEEDBACK", feedbackId)
                        );
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to notify agent {} for subscription request {}: {}", agentId, feedbackId, e.getMessage());
            }
        }
    }

    private FeedbackDTO toDTO(UserFeedback f) {
        return FeedbackDTO.builder()
                .id(f.getId())
                .tenantId(f.getTenantId())
                .username(f.getUsername())
                .type(f.getType())
                .typeDisplayName(f.getType().getDisplayName())
                .title(f.getTitle())
                .content(f.getContent())
                .status(f.getStatus())
                .statusDisplayName(f.getStatus().getDisplayName())
                .adminNote(f.getAdminNote())
                .resolvedAt(f.getResolvedAt())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}

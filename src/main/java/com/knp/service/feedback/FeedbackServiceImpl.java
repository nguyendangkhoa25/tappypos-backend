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

    @Override
    @Transactional
    public FeedbackDTO create(CreateFeedbackRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
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

        return toDTO(feedbackRepository.save(feedback));
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
        return toDTO(feedbackRepository.save(feedback));
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

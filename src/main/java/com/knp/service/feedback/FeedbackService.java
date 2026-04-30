package com.knp.service.feedback;

import com.knp.model.dto.feedback.CreateFeedbackRequest;
import com.knp.model.dto.feedback.FeedbackDTO;
import com.knp.model.dto.feedback.UpdateFeedbackRequest;
import com.knp.model.enums.FeedbackStatus;
import com.knp.model.enums.FeedbackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    FeedbackDTO create(CreateFeedbackRequest request);
    Page<FeedbackDTO> getMyFeedback(Pageable pageable);
    Page<FeedbackDTO> getAllFeedback(String tenantId, FeedbackStatus status, FeedbackType type, Pageable pageable);
    FeedbackDTO updateStatus(Long id, UpdateFeedbackRequest request);
}

package com.tappy.pos.service.feedback;

import com.tappy.pos.model.dto.feedback.CreateFeedbackRequest;
import com.tappy.pos.model.dto.feedback.FeedbackDTO;
import com.tappy.pos.model.dto.feedback.UpdateFeedbackRequest;
import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    FeedbackDTO create(CreateFeedbackRequest request);
    Page<FeedbackDTO> getMyFeedback(Pageable pageable);
    Page<FeedbackDTO> getAllFeedback(String tenantId, FeedbackStatus status, FeedbackType type, Pageable pageable);
    FeedbackDTO updateStatus(Long id, UpdateFeedbackRequest request);
}

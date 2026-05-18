package com.tappy.pos.model.dto.feedback;

import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackDTO {
    private Long id;
    private String tenantId;
    private String username;
    private FeedbackType type;
    private String typeDisplayName;
    private String title;
    private String content;
    private FeedbackStatus status;
    private String statusDisplayName;
    private String adminNote;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

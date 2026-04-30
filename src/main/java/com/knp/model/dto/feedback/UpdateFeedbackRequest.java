package com.knp.model.dto.feedback;

import com.knp.model.enums.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFeedbackRequest {

    @NotNull
    private FeedbackStatus status;

    @Size(max = 1000)
    private String adminNote;
}

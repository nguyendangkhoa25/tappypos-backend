package com.tappy.pos.model.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateNotificationRequest {

    /**
     * Target usernames. Empty/null = broadcast to all active users in this tenant.
     */
    private List<String> targetUserIds;

    @NotBlank(message = "Title is required")
    private String title;

    private String message;

    /** SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO — defaults to INFO */
    private String type;

    private String referenceType;
    private Long referenceId;
}

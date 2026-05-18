package com.tappy.pos.model.entity.feedback;

import com.tappy.pos.model.entity.BaseEntity;
import com.tappy.pos.model.enums.FeedbackStatus;
import com.tappy.pos.model.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedback")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserFeedback extends BaseEntity {

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private FeedbackType type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}

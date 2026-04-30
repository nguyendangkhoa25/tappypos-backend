package com.knp.model.enums;

import lombok.Getter;

@Getter
public enum FeedbackStatus {
    PENDING("Chờ xử lý", "Pending"),
    IN_REVIEW("Đang xem xét", "In Review"),
    RESOLVED("Đã giải quyết", "Resolved"),
    CLOSED("Đã đóng", "Closed");

    private final String displayName;
    private final String englishName;

    FeedbackStatus(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }

}

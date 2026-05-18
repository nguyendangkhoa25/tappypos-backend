package com.tappy.pos.model.enums;

import lombok.Getter;

@Getter
public enum FeedbackType {
    FEEDBACK("Phản hồi", "Feedback"),
    BUG_REPORT("Báo lỗi", "Bug Report"),
    SUGGESTION("Góp ý", "Suggestion"),
    QUESTION("Câu hỏi", "Question"),
    SUBSCRIPTION_REQUEST("Yêu cầu gói dịch vụ", "Subscription Request");

    private final String displayName;
    private final String englishName;

    FeedbackType(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }

}

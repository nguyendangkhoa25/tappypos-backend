package com.knp.model.enums;

import lombok.Getter;

@Getter
public enum FeedbackType {
    FEEDBACK("Phản hồi", "Feedback"),
    BUG_REPORT("Báo lỗi", "Bug Report"),
    SUGGESTION("Góp ý", "Suggestion"),
    QUESTION("Câu hỏi", "Question");

    private final String displayName;
    private final String englishName;

    FeedbackType(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }

}

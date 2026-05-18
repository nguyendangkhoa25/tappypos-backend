package com.tappy.pos.model.enums;

import lombok.Getter;

@Getter
public enum LeadStatus {
    NEW("Mới", "New"),
    CONTACTED("Đã liên hệ", "Contacted"),
    CONVERTED("Đã chuyển đổi", "Converted"),
    CLOSED("Đã đóng", "Closed");

    private final String displayName;
    private final String englishName;

    LeadStatus(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }
}

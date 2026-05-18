package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * SubscriptionType - Enum for tenant subscription types
 */
@Getter
public enum SubscriptionType {
    TRIAL("Dùng thử"),
    STARTER("Khởi đầu"),
    BASIC("Cơ bản"),
    PRO("Chuyên nghiệp"),
    ENTERPRISE("Doanh nghiệp");

    private final String displayName;

    SubscriptionType(String displayName) {
        this.displayName = displayName;
    }

    public String getTypeName() {
        return this.name();
    }
}


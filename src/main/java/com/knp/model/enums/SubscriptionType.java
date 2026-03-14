package com.knp.model.enums;

import lombok.Getter;

/**
 * SubscriptionType - Enum for tenant subscription types
 */
@Getter
public enum SubscriptionType {
    TRIAL("Gói Dùng thử"),
    BASIC("Gói Cơ bản"),
    PREMIUM("Gói Cao cấp"),
    ENTERPRISE("Gói Doanh nghiệp lớn");

    private final String description;

    SubscriptionType(String description) {
        this.description = description;
    }

    public String getTypeName() {
        return this.name();
    }
}


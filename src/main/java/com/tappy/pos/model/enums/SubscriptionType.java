package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * SubscriptionType - Enum for tenant subscription packages.
 *
 * Unified catalog (see docs/SUBSCRIPTION_PRICING_PLAN.md):
 *   - TRIAL       Dùng thử — free first year, 2 users, full features
 *   - BASIC       Gói Cơ bản — 2 users
 *   - PRO         Gói Chuyên nghiệp — 6 users
 *   - ENTERPRISE  Gói Doanh nghiệp — 15+ users / multi-branch
 *   - GOLD_PAWN   Gói Vàng & Cầm đồ — vertical package for jewelry/pawn (2 users)
 *
 * Legacy codes STARTER/PREMIUM were removed; existing rows are remapped by Flyway
 * (STARTER→BASIC, PREMIUM→PRO) in V042__unify_subscription_catalog.sql.
 */
@Getter
public enum SubscriptionType {
    TRIAL("Dùng thử"),
    BASIC("Cơ bản"),
    PRO("Chuyên nghiệp"),
    ENTERPRISE("Doanh nghiệp"),
    GOLD_PAWN("Vàng & Cầm đồ");

    private final String displayName;

    SubscriptionType(String displayName) {
        this.displayName = displayName;
    }

    public String getTypeName() {
        return this.name();
    }
}


package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * SubscriptionType - Enum for tenant subscription packages.
 *
 * Unified catalog (see docs/SUBSCRIPTION_PRICING_PLAN.md):
 *   - STARTER     Gói Khởi nghiệp — 1 user (solo micro-shop)
 *   - BASIC       Gói Cơ bản — 2 users (default plan; free first 6 months)
 *   - PRO         Gói Chuyên nghiệp — 4 users
 *   - ENTERPRISE  Gói Doanh nghiệp — 10 users / multi-branch
 *   - GOLD_PAWN   Gói Vàng & Cầm đồ — vertical package for jewelry/pawn (2 users)
 *
 * TRIAL was removed (2026-06): every new shop now starts on BASIC, free for 6 months
 * (see SubscriptionPlan.FREE_TRIAL_MONTHS), then goes read-only. Existing TRIAL rows are
 * remapped to BASIC by Flyway in V003__subscription_catalog_rework.sql. STARTER is now a
 * distinct 1-user tier (the old STARTER→BASIC remap predates this re-introduction).
 */
@Getter
public enum SubscriptionType {
    STARTER("Khởi nghiệp"),
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


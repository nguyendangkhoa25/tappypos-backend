package com.tappy.pos.model.enums;

import java.util.Map;

public final class SubscriptionPlan {

    private SubscriptionPlan() {}

    public record PlanLimits(Integer maxUsers, Integer maxOrdersPerMonth, long pricePerMonth, long pricePerYear) {
        public boolean isOrderUnlimited() { return maxOrdersPerMonth == null; }
        public boolean isUserUnlimited()  { return maxUsers == null; }
    }

    /** Default plan a freshly-registered shop is placed on (free for {@link #FREE_TRIAL_MONTHS}). */
    public static final String DEFAULT_PLAN = "BASIC";

    /** Free period granted to every new shop at registration, regardless of shop type. */
    public static final int FREE_TRIAL_MONTHS = 6;

    // Prices in VND. Single source of truth for plan limits/pricing — mirrored
    // by frontend SUBSCRIPTION_TIERS and mobile UpgradeModal PLANS.
    // Orders are unlimited on every package: pricing is on users, not order volume.
    // Annual price = 10 × monthly (2 months free). TRIAL was removed (2026-06):
    // every new shop starts on BASIC, free for 6 months, then goes read-only.
    public static final Map<String, PlanLimits> LIMITS = Map.of(
        "STARTER",    new PlanLimits(1,    null,  49_000,    490_000),    // Gói Khởi nghiệp (1 người)
        "BASIC",      new PlanLimits(2,    null,  99_000,    990_000),    // Gói Cơ bản (mặc định, free 6 tháng)
        "PRO",        new PlanLimits(4,    null,  199_000,   1_990_000),  // Gói Chuyên nghiệp
        "ENTERPRISE", new PlanLimits(10,   null,  399_000,   3_990_000),  // Gói Doanh nghiệp
        "GOLD_PAWN",  new PlanLimits(2,    null,  199_000,   1_990_000)   // Gói Vàng & Cầm đồ
    );

    /**
     * Resolve plan limits for a subscription code. A null/blank code means "no plan set"
     * and falls back to {@link #DEFAULT_PLAN}. An unknown non-blank code is a data-integrity
     * error and fails loudly rather than silently masquerading as the default.
     */
    public static PlanLimits of(String subscriptionType) {
        if (subscriptionType == null || subscriptionType.isBlank()) return LIMITS.get(DEFAULT_PLAN);
        PlanLimits limits = LIMITS.get(subscriptionType.toUpperCase());
        if (limits == null) {
            throw new IllegalArgumentException("Unknown subscription plan code: " + subscriptionType);
        }
        return limits;
    }

    /** True if the code is a known plan (or null/blank, which maps to the default). Use to validate writes. */
    public static boolean isValid(String subscriptionType) {
        return subscriptionType == null || subscriptionType.isBlank()
                || LIMITS.containsKey(subscriptionType.toUpperCase());
    }
}

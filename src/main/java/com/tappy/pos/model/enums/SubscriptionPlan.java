package com.tappy.pos.model.enums;

import java.util.Map;

public final class SubscriptionPlan {

    private SubscriptionPlan() {}

    public record PlanLimits(Integer maxUsers, Integer maxOrdersPerMonth, long pricePerMonth) {
        public boolean isOrderUnlimited() { return maxOrdersPerMonth == null; }
        public boolean isUserUnlimited()  { return maxUsers == null; }
    }

    // Prices in VND. Single source of truth for plan limits/pricing — mirrored
    // by frontend SUBSCRIPTION_TIERS and mobile UpgradeModal PLANS.
    // Orders are unlimited on every package: pricing is on users, not order volume
    // (a soft monthly-order cap on the free plan is a pending product decision).
    public static final Map<String, PlanLimits> LIMITS = Map.of(
        "TRIAL",      new PlanLimits(2,    null,  0),       // Dùng thử (free first year)
        "BASIC",      new PlanLimits(2,    null,  99_000),  // Gói Cơ bản
        "PRO",        new PlanLimits(6,    null,  199_000), // Gói Chuyên nghiệp
        "ENTERPRISE", new PlanLimits(15,   null,  399_000), // Gói Doanh nghiệp
        "GOLD_PAWN",  new PlanLimits(2,    null,  199_000)  // Gói Vàng & Cầm đồ
    );

    /**
     * Resolve plan limits for a subscription code. A null/blank code means "no plan set"
     * and falls back to TRIAL. An unknown non-blank code is a data-integrity error and
     * fails loudly rather than silently masquerading as TRIAL.
     */
    public static PlanLimits of(String subscriptionType) {
        if (subscriptionType == null || subscriptionType.isBlank()) return LIMITS.get("TRIAL");
        PlanLimits limits = LIMITS.get(subscriptionType.toUpperCase());
        if (limits == null) {
            throw new IllegalArgumentException("Unknown subscription plan code: " + subscriptionType);
        }
        return limits;
    }

    /** True if the code is a known plan (or null/blank, which maps to TRIAL). Use to validate writes. */
    public static boolean isValid(String subscriptionType) {
        return subscriptionType == null || subscriptionType.isBlank()
                || LIMITS.containsKey(subscriptionType.toUpperCase());
    }
}

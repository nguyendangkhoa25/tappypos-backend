package com.tappy.pos.model.enums;

import java.util.Map;

public final class SubscriptionPlan {

    private SubscriptionPlan() {}

    public record PlanLimits(Integer maxUsers, Integer maxOrdersPerMonth, long pricePerMonth) {
        public boolean isOrderUnlimited() { return maxOrdersPerMonth == null; }
        public boolean isUserUnlimited()  { return maxUsers == null; }
    }

    // Prices in VND
    public static final Map<String, PlanLimits> LIMITS = Map.of(
        "TRIAL",      new PlanLimits(1,    1_000,  0),
        "STARTER",    new PlanLimits(1,    1_000,  70_000),
        "BASIC",      new PlanLimits(3,    5_000,  100_000),
        "PRO",        new PlanLimits(10,   null,   300_000),
        "ENTERPRISE", new PlanLimits(null, null,   0)
    );

    public static PlanLimits of(String subscriptionType) {
        if (subscriptionType == null) return LIMITS.get("TRIAL");
        return LIMITS.getOrDefault(subscriptionType.toUpperCase(), LIMITS.get("TRIAL"));
    }
}

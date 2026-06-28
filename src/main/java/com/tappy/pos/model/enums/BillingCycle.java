package com.tappy.pos.model.enums;

import com.tappy.pos.model.enums.SubscriptionPlan.PlanLimits;

import java.time.LocalDate;

/**
 * Billing cycle for a subscription payment. The amount is always derived from
 * {@link SubscriptionPlan} (server-side) — never trusted from the client.
 */
public enum BillingCycle {
    MONTHLY,
    YEARLY;

    /** Price (VND) for this cycle, from the plan's limits. */
    public long amountFor(PlanLimits limits) {
        return this == YEARLY ? limits.pricePerYear() : limits.pricePerMonth();
    }

    /** Extend a subscription from the given date by one cycle. */
    public LocalDate extend(LocalDate from) {
        return this == YEARLY ? from.plusYears(1) : from.plusMonths(1);
    }
}

package com.tappy.pos.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Platform-wide subscription billing stats for the master Billing &amp; Revenue cockpit.
 * All amounts are VND (đồng), no decimals. Aggregated across all tenants from the (RLS-free)
 * {@code subscription_payment} table; MRR is derived from active, non-expired tenants' plans.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterBillingStatsDTO {

    private long revenueToday;
    private long revenueThisMonth;
    private long revenueThisYear;
    private long paidCountThisMonth;
    private long pendingCount;
    private long failedCount;
    /** Monthly Recurring Revenue: sum of active tenants' plan monthly-equivalent price (VND). */
    private long mrr;

    private List<PlanRevenue> byPlan;
    private List<ProviderRevenue> byProvider;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanRevenue {
        private String planCode;
        private long count;
        private long amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderRevenue {
        private String provider;
        private long count;
        private long amount;
    }
}

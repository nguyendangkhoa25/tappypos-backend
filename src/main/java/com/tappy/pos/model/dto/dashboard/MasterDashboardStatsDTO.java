package com.tappy.pos.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDashboardStatsDTO {

    private TenantStats tenants;
    private AgentStats agents;
    private ProductCatalogStats productCatalog;
    private FeedbackStats feedback;
    private ContactLeadStats contactLeads;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantStats {
        private long total;
        private long active;
        private long expiringSoon;
        private long expired;
        private long inactive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStats {
        private long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCatalogStats {
        private long total;
        private long fromOpenFoodFacts;
        private long manual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackStats {
        private long total;
        private long pending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactLeadStats {
        private long total;
        private long newLeads;
    }
}

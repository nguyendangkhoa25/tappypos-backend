package com.knp.controller.dashboard;

import com.knp.annotation.RequiresFeature;
import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.dashboard.MasterDashboardStatsDTO;
import com.knp.model.dto.tenant.TenantStatsDTO;
import com.knp.model.enums.FeedbackStatus;
import com.knp.model.enums.LeadStatus;
import com.knp.repository.contact.ContactLeadRepository;
import com.knp.repository.feedback.FeedbackRepository;
import com.knp.repository.product.ProductCatalogRepository;
import com.knp.repository.tenant.AgentRepository;
import com.knp.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/master-dashboard")
@RequiredArgsConstructor
@RequiresFeature("MASTER_DASHBOARD")
public class MasterDashboardController {

    private final TenantService tenantService;
    private final AgentRepository agentRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final FeedbackRepository feedbackRepository;
    private final ContactLeadRepository contactLeadRepository;

    /**
     * GET /api/master-dashboard/stats
     * Returns aggregated platform statistics for master admin and agent dashboards.
     * Accessible to anyone with MASTER_DASHBOARD feature (master admin + agents).
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MasterDashboardStatsDTO>> getStats() {
        log.info("Request: GET /master-dashboard/stats");

        // Tenant stats — scoped to caller's visible tenants (AGENT sees only their own)
        TenantStatsDTO tenantStats = tenantService.getStats();

        // Agent count — only master admin needs this; agents won't show it in UI
        long agentTotal = agentRepository.count();

        // Product catalog stats
        long catalogTotal        = productCatalogRepository.count();
        long catalogFromOff      = productCatalogRepository.countBySource("OPEN_FOOD_FACTS");
        long catalogManual       = catalogTotal - catalogFromOff;

        // Feedback stats
        long feedbackTotal   = feedbackRepository.countAllActive();
        long feedbackPending = feedbackRepository.countByStatus(FeedbackStatus.PENDING);

        // Contact lead stats
        long leadsTotal    = contactLeadRepository.countAllActive();
        long leadsNew      = contactLeadRepository.countByStatus(LeadStatus.NEW);

        MasterDashboardStatsDTO stats = MasterDashboardStatsDTO.builder()
                .tenants(MasterDashboardStatsDTO.TenantStats.builder()
                        .total(tenantStats.getTotal())
                        .active(tenantStats.getActive())
                        .expiringSoon(tenantStats.getExpiringSoon())
                        .expired(tenantStats.getExpired())
                        .inactive(tenantStats.getInactive())
                        .build())
                .agents(MasterDashboardStatsDTO.AgentStats.builder()
                        .total(agentTotal)
                        .build())
                .productCatalog(MasterDashboardStatsDTO.ProductCatalogStats.builder()
                        .total(catalogTotal)
                        .fromOpenFoodFacts(catalogFromOff)
                        .manual(catalogManual)
                        .build())
                .feedback(MasterDashboardStatsDTO.FeedbackStats.builder()
                        .total(feedbackTotal)
                        .pending(feedbackPending)
                        .build())
                .contactLeads(MasterDashboardStatsDTO.ContactLeadStats.builder()
                        .total(leadsTotal)
                        .newLeads(leadsNew)
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved successfully"));
    }
}

package com.tappy.pos.service.tax;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.revenue.RevenuePeriodDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationRequest;
import com.tappy.pos.model.dto.tax.TaxEstimateDTO;
import com.tappy.pos.model.entity.finance.TaxDeclaration;
import com.tappy.pos.model.entity.finance.TaxRateCatalog;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.BusinessType;
import com.tappy.pos.model.enums.TaxDeclarationStatus;
import com.tappy.pos.model.enums.TaxPeriodType;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tax.TaxDeclarationRepository;
import com.tappy.pos.repository.tax.TaxRateCatalogRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.finance.RevenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxDeclarationService Unit Tests")
class TaxDeclarationServiceImplTest {

    @Mock private TaxDeclarationRepository declarationRepository;
    @Mock private TaxRateCatalogRepository rateCatalogRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private RevenueService revenueService;
    @Mock private TenantContext tenantContext;
    @Mock private AuthContext authContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks
    private TaxDeclarationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        lenient().when(authContext.getCurrentUsername()).thenReturn("owner");
    }

    private ShopInfo shop(BusinessType type, String groups) {
        return ShopInfo.builder().shopName("Test").businessType(type).taxIndustryGroups(groups).build();
    }

    private TaxRateCatalog distribution() {
        return TaxRateCatalog.builder()
                .code("DISTRIBUTION").name("Phân phối, cung cấp hàng hóa")
                .vatRate(new BigDecimal("1.00")).pitRate(new BigDecimal("0.50"))
                .effectiveFrom(java.time.LocalDate.of(2026, 1, 1)).active(true).build();
    }

    // ── estimate ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("estimate: ENTERPRISE → businessTypeSupported=false (coming soon)")
    void estimate_enterprise_notSupported() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.ENTERPRISE, "DISTRIBUTION")));

        TaxEstimateDTO est = service.estimate("QUARTER", 2026, 2);

        assertThat(est.isBusinessTypeSupported()).isFalse();
        assertThat(est.getBusinessType()).isEqualTo("ENTERPRISE");
        verify(revenueService, never()).getMonthlyBreakdown(anyInt());
    }

    @Test
    @DisplayName("estimate: no industry groups → needsSetup=true")
    void estimate_noGroups_needsSetup() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.HOUSEHOLD, null)));

        TaxEstimateDTO est = service.estimate("QUARTER", 2026, 2);

        assertThat(est.isNeedsSetup()).isTrue();
    }

    @Test
    @DisplayName("estimate: HOUSEHOLD + 1 group → sums quarter revenue and applies rates")
    void estimate_household_computesTax() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.HOUSEHOLD, "DISTRIBUTION")));
        when(rateCatalogRepository.findByCodeAndDeletedFalse("DISTRIBUTION"))
                .thenReturn(Optional.of(distribution()));
        // Q1 = months 1,2,3
        when(revenueService.getMonthlyBreakdown(2026)).thenReturn(List.of(
                period(1, "40000000"), period(2, "30000000"), period(3, "30000000"), period(4, "99999999")));
        when(declarationRepository.findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(
                any(), eq(2026), eq(1))).thenReturn(Optional.empty());

        TaxEstimateDTO est = service.estimate("QUARTER", 2026, 1);

        assertThat(est.getAutoRevenue()).isEqualByComparingTo("100000000"); // 40+30+30M (month 4 excluded)
        assertThat(est.getTotalVat()).isEqualByComparingTo("1000000");      // 1%
        assertThat(est.getTotalPit()).isEqualByComparingTo("500000");       // 0.5%
        assertThat(est.getTotalTax()).isEqualByComparingTo("1500000");
        assertThat(est.getLines()).hasSize(1);
    }

    private RevenuePeriodDTO period(int month, String revenue) {
        return RevenuePeriodDTO.builder().period(month).revenue(new BigDecimal(revenue)).build();
    }

    // ── createDraft guards ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createDraft: businessType null → BadRequest")
    void createDraft_noBusinessType_throws() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(null, null)));

        assertThatThrownBy(() -> service.createDraft(new TaxDeclarationRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.tax.business_type_required");
    }

    @Test
    @DisplayName("createDraft: ENTERPRISE → BadRequest (not supported)")
    void createDraft_enterprise_throws() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.ENTERPRISE, "DISTRIBUTION")));

        assertThatThrownBy(() -> service.createDraft(new TaxDeclarationRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.tax.enterprise_not_supported");
    }

    @Test
    @DisplayName("createDraft: HOUSEHOLD → snapshots rates, computes totals, saves DRAFT")
    void createDraft_household_savesWithTotals() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.HOUSEHOLD, "DISTRIBUTION")));
        when(rateCatalogRepository.findByCodeAndDeletedFalse("DISTRIBUTION"))
                .thenReturn(Optional.of(distribution()));
        when(revenueService.getMonthlyBreakdown(2026)).thenReturn(List.of());
        when(declarationRepository.findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(
                any(), eq(2026), eq(2))).thenReturn(Optional.empty());
        when(declarationRepository.save(any(TaxDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxDeclarationRequest req = TaxDeclarationRequest.builder()
                .periodType("QUARTER").periodYear(2026).periodNumber(2)
                .lines(List.of(TaxDeclarationRequest.LineRequest.builder()
                        .industryCode("DISTRIBUTION").revenue(new BigDecimal("100000000")).build()))
                .build();

        TaxDeclarationDTO dto = service.createDraft(req);

        assertThat(dto.getStatus()).isEqualTo("DRAFT");
        assertThat(dto.getDeclaredRevenue()).isEqualByComparingTo("100000000");
        assertThat(dto.getTotalVat()).isEqualByComparingTo("1000000");
        assertThat(dto.getTotalPit()).isEqualByComparingTo("500000");
        assertThat(dto.getTotalTax()).isEqualByComparingTo("1500000");
        assertThat(dto.getLines()).hasSize(1);
        verify(declarationRepository).save(any(TaxDeclaration.class));
    }

    @Test
    @DisplayName("createDraft: duplicate period → BadRequest")
    void createDraft_duplicatePeriod_throws() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shop(BusinessType.HOUSEHOLD, "DISTRIBUTION")));
        when(declarationRepository.findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(
                any(), eq(2026), eq(2))).thenReturn(Optional.of(new TaxDeclaration()));

        TaxDeclarationRequest req = TaxDeclarationRequest.builder()
                .periodType("QUARTER").periodYear(2026).periodNumber(2).lines(List.of()).build();

        assertThatThrownBy(() -> service.createDraft(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.tax.period_exists");
    }

    // ── status transitions ──────────────────────────────────────────────────────

    @Test
    @DisplayName("finalize: DRAFT → FINALIZED")
    void finalize_draft_ok() {
        TaxDeclaration d = draft();
        when(declarationRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(d));
        when(declarationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaxDeclarationDTO dto = service.finalizeDeclaration(5L);

        assertThat(dto.getStatus()).isEqualTo("FINALIZED");
    }

    @Test
    @DisplayName("finalize: non-draft → BadRequest")
    void finalize_nonDraft_throws() {
        TaxDeclaration d = draft();
        d.setStatus(TaxDeclarationStatus.SUBMITTED);
        when(declarationRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.finalizeDeclaration(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.tax.not_editable");
    }

    @Test
    @DisplayName("markSubmitted: DRAFT → BadRequest (must finalize first)")
    void submit_draft_throws() {
        TaxDeclaration d = draft();
        when(declarationRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.markSubmitted(5L, "REF-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.tax.finalize_before_submit");
    }

    @Test
    @DisplayName("markSubmitted: FINALIZED → SUBMITTED with gov ref")
    void submit_finalized_ok() {
        TaxDeclaration d = draft();
        d.setStatus(TaxDeclarationStatus.FINALIZED);
        when(declarationRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(d));
        when(declarationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaxDeclarationDTO dto = service.markSubmitted(5L, "REF-123");

        assertThat(dto.getStatus()).isEqualTo("SUBMITTED");
        assertThat(dto.getGovRefNumber()).isEqualTo("REF-123");
        assertThat(dto.getSubmittedAt()).isNotNull();
    }

    private TaxDeclaration draft() {
        TaxDeclaration d = TaxDeclaration.builder()
                .periodType(TaxPeriodType.QUARTER).periodYear(2026).periodNumber(2)
                .businessType(BusinessType.HOUSEHOLD).status(TaxDeclarationStatus.DRAFT)
                .build();
        d.setTenantId("shop-1");
        return d;
    }
}

package com.tappy.pos.service.tax;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.revenue.RevenuePeriodDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationLineDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationRequest;
import com.tappy.pos.model.dto.tax.TaxEstimateDTO;
import com.tappy.pos.model.dto.tax.TaxRateCatalogDTO;
import com.tappy.pos.model.entity.finance.TaxDeclaration;
import com.tappy.pos.model.entity.finance.TaxDeclarationLine;
import com.tappy.pos.model.entity.finance.TaxRateCatalog;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ActivityAction;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaxDeclarationServiceImpl implements TaxDeclarationService {

    private final TaxDeclarationRepository declarationRepository;
    private final TaxRateCatalogRepository rateCatalogRepository;
    private final ShopInfoRepository shopInfoRepository;
    private final RevenueService revenueService;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    // ── Read ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TaxRateCatalogDTO> getRateCatalog() {
        return rateCatalogRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc()
                .stream().map(this::toCatalogDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaxEstimateDTO estimate(String periodType, int year, int number) {
        TaxPeriodType type = parsePeriodType(periodType);
        ShopInfo shop = currentShopInfo();
        BusinessType bt = shop != null ? shop.getBusinessType() : null;

        TaxEstimateDTO.TaxEstimateDTOBuilder b = TaxEstimateDTO.builder()
                .periodType(type.name())
                .periodYear(year)
                .periodNumber(number)
                .periodLabel(periodLabel(type, year, number))
                .businessType(bt != null ? bt.name() : null)
                .businessTypeSupported(bt != null && bt.isTaxDeclarationSupported())
                .autoRevenue(BigDecimal.ZERO)
                .totalVat(BigDecimal.ZERO)
                .totalPit(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .lines(new ArrayList<>());

        // Loại hình chưa hỗ trợ (công ty) → frontend hiển thị màn "sắp có".
        if (bt != null && !bt.isTaxDeclarationSupported()) {
            return b.build();
        }
        // Chưa chọn loại hình / nhóm ngành → cần onboarding trước.
        List<String> groups = parseIndustryGroups(shop);
        if (bt == null || groups.isEmpty()) {
            return b.needsSetup(true).build();
        }

        BigDecimal autoRevenue = autoRevenueForPeriod(type, year, number);

        // Doanh thu auto dồn vào nhóm ngành chính (nhóm đầu); các nhóm còn lại 0 — chủ shop
        // điều chỉnh khi tạo tờ khai. (Không thể tự bóc tách doanh thu theo ngành từ POS.)
        List<TaxDeclarationLineDTO> lines = new ArrayList<>();
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalPit = BigDecimal.ZERO;
        for (int i = 0; i < groups.size(); i++) {
            TaxRateCatalog cat = rateCatalogRepository.findByCodeAndDeletedFalse(groups.get(i)).orElse(null);
            if (cat == null) continue;
            BigDecimal revenue = (i == 0) ? autoRevenue : BigDecimal.ZERO;
            BigDecimal vat = taxAmount(revenue, cat.getVatRate());
            BigDecimal pit = taxAmount(revenue, cat.getPitRate());
            totalVat = totalVat.add(vat);
            totalPit = totalPit.add(pit);
            lines.add(TaxDeclarationLineDTO.builder()
                    .industryCode(cat.getCode())
                    .industryName(cat.getName())
                    .revenue(revenue)
                    .vatRate(cat.getVatRate())
                    .pitRate(cat.getPitRate())
                    .vatAmount(vat)
                    .pitAmount(pit)
                    .build());
        }

        Optional<TaxDeclaration> existing = declarationRepository
                .findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(type, year, number);

        return b.autoRevenue(autoRevenue)
                .totalVat(totalVat)
                .totalPit(totalPit)
                .totalTax(totalVat.add(totalPit))
                .lines(lines)
                .existingDeclarationId(existing.map(TaxDeclaration::getId).orElse(null))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaxDeclarationDTO> list(Integer year, Pageable pageable) {
        Page<TaxDeclaration> page = (year != null)
                ? declarationRepository.findByPeriodYearAndDeletedFalseOrderByPeriodNumberDesc(year, pageable)
                : declarationRepository.findByDeletedFalseOrderByPeriodYearDescPeriodNumberDesc(pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxDeclarationDTO getById(Long id) {
        return toDTO(requireDeclaration(id));
    }

    // ── Write ──────────────────────────────────────────────────────────────

    @Override
    public TaxDeclarationDTO createDraft(TaxDeclarationRequest request) {
        ShopInfo shop = currentShopInfo();
        BusinessType bt = shop != null ? shop.getBusinessType() : null;
        if (bt == null) {
            throw new BadRequestException(messageService.getMessage("error.tax.business_type_required"));
        }
        if (!bt.isTaxDeclarationSupported()) {
            throw new BadRequestException(messageService.getMessage("error.tax.enterprise_not_supported"));
        }

        TaxPeriodType type = parsePeriodType(request.getPeriodType());
        Integer year = request.getPeriodYear();
        Integer number = request.getPeriodNumber();
        if (year == null || number == null) {
            throw new BadRequestException(messageService.getMessage("error.tax.period_required"));
        }
        declarationRepository.findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(type, year, number)
                .ifPresent(d -> { throw new BadRequestException(messageService.getMessage("error.tax.period_exists")); });

        TaxDeclaration decl = TaxDeclaration.builder()
                .periodType(type)
                .periodYear(year)
                .periodNumber(number)
                .businessType(bt)
                .autoRevenue(autoRevenueForPeriod(type, year, number))
                .status(TaxDeclarationStatus.DRAFT)
                .notes(request.getNotes())
                .createdBy(authContext.getCurrentUsername())
                .build();
        decl.setTenantId(tenantContext.getCurrentTenantId());

        applyLines(decl, request.getLines());
        TaxDeclaration saved = declarationRepository.save(decl);

        logActivity(saved, ActivityAction.TAX_DECLARATION_CREATED, "activity.tax.created");
        return toDTO(saved);
    }

    @Override
    public TaxDeclarationDTO update(Long id, TaxDeclarationRequest request) {
        TaxDeclaration decl = requireDeclaration(id);
        if (decl.getStatus() != TaxDeclarationStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.tax.not_editable"));
        }
        if (request.getNotes() != null) decl.setNotes(request.getNotes());
        if (request.getLines() != null) {
            decl.getLines().clear();
            applyLines(decl, request.getLines());
        }
        return toDTO(declarationRepository.save(decl));
    }

    @Override
    public TaxDeclarationDTO finalizeDeclaration(Long id) {
        TaxDeclaration decl = requireDeclaration(id);
        if (decl.getStatus() != TaxDeclarationStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.tax.not_editable"));
        }
        decl.setStatus(TaxDeclarationStatus.FINALIZED);
        TaxDeclaration saved = declarationRepository.save(decl);
        logActivity(saved, ActivityAction.TAX_DECLARATION_FINALIZED, "activity.tax.finalized");
        return toDTO(saved);
    }

    @Override
    public TaxDeclarationDTO markSubmitted(Long id, String govRefNumber) {
        TaxDeclaration decl = requireDeclaration(id);
        if (decl.getStatus() == TaxDeclarationStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.tax.finalize_before_submit"));
        }
        decl.setStatus(TaxDeclarationStatus.SUBMITTED);
        decl.setGovRefNumber(govRefNumber);
        decl.setSubmittedAt(LocalDateTime.now());
        TaxDeclaration saved = declarationRepository.save(decl);
        logActivity(saved, ActivityAction.TAX_DECLARATION_SUBMITTED, "activity.tax.submitted");
        return toDTO(saved);
    }

    @Override
    public void cancel(Long id) {
        TaxDeclaration decl = requireDeclaration(id);
        decl.softDelete();
        decl.setStatus(TaxDeclarationStatus.CANCELLED);
        declarationRepository.save(decl);
        logActivity(decl, ActivityAction.TAX_DECLARATION_CANCELLED, "activity.tax.cancelled");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPrintableHtml(Long id) {
        TaxDeclaration decl = requireDeclaration(id);
        ShopInfo shop = currentShopInfo();
        return TaxFormHtmlBuilder.build(decl, shop).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applyLines(TaxDeclaration decl, List<TaxDeclarationRequest.LineRequest> lineRequests) {
        if (lineRequests == null) return;
        String tenantId = tenantContext.getCurrentTenantId();
        for (TaxDeclarationRequest.LineRequest lr : lineRequests) {
            if (lr.getIndustryCode() == null) continue;
            TaxRateCatalog cat = rateCatalogRepository.findByCodeAndDeletedFalse(lr.getIndustryCode())
                    .orElseThrow(() -> new BadRequestException(
                            messageService.getMessage("error.tax.industry_not_found")));
            BigDecimal revenue = lr.getRevenue() != null ? lr.getRevenue() : BigDecimal.ZERO;
            TaxDeclarationLine line = TaxDeclarationLine.builder()
                    .declaration(decl)
                    .industryCode(cat.getCode())
                    .industryName(cat.getName())
                    .revenue(revenue)
                    .vatRate(cat.getVatRate())
                    .pitRate(cat.getPitRate())
                    .vatAmount(taxAmount(revenue, cat.getVatRate()))
                    .pitAmount(taxAmount(revenue, cat.getPitRate()))
                    .build();
            line.setTenantId(tenantId);
            decl.getLines().add(line);
        }
        decl.setDeclaredRevenue(decl.getLines().stream()
                .map(TaxDeclarationLine::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        decl.recomputeTotals();
    }

    /** Doanh thu auto từ POS cho kỳ — tổng doanh thu các tháng trong quý/kỳ. */
    private BigDecimal autoRevenueForPeriod(TaxPeriodType type, int year, int number) {
        List<RevenuePeriodDTO> monthly = revenueService.getMonthlyBreakdown(year);
        int startMonth;
        int endMonth;
        switch (type) {
            case QUARTER -> { startMonth = (number - 1) * 3 + 1; endMonth = number * 3; }
            case MONTH -> { startMonth = number; endMonth = number; }
            default -> { startMonth = 1; endMonth = 12; } // YEAR
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (RevenuePeriodDTO p : monthly) {
            if (p.getPeriod() != null && p.getPeriod() >= startMonth && p.getPeriod() <= endMonth
                    && p.getRevenue() != null) {
                sum = sum.add(p.getRevenue());
            }
        }
        return sum;
    }

    /** revenue × rate% làm tròn đến đồng (HALF_UP). */
    private BigDecimal taxAmount(BigDecimal revenue, BigDecimal ratePercent) {
        if (revenue == null || ratePercent == null) return BigDecimal.ZERO;
        return revenue.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> parseIndustryGroups(ShopInfo shop) {
        if (shop == null || shop.getTaxIndustryGroups() == null || shop.getTaxIndustryGroups().isBlank()) {
            return List.of();
        }
        return Arrays.stream(shop.getTaxIndustryGroups().split(","))
                .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private ShopInfo currentShopInfo() {
        return shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc().orElse(null);
    }

    private TaxDeclaration requireDeclaration(Long id) {
        return declarationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.tax.declaration_not_found")));
    }

    private TaxPeriodType parsePeriodType(String raw) {
        if (raw == null || raw.isBlank()) return TaxPeriodType.QUARTER;
        try {
            return TaxPeriodType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TaxPeriodType.QUARTER;
        }
    }

    private String periodLabel(TaxPeriodType type, int year, int number) {
        return switch (type) {
            case QUARTER -> "Quý " + number + "/" + year;
            case MONTH -> "Tháng " + number + "/" + year;
            case YEAR -> "Năm " + year;
        };
    }

    private void logActivity(TaxDeclaration decl, ActivityAction action, String messageKey) {
        activityLogService.logAsync(
                tenantContext.getCurrentTenantId(),
                authContext.getCurrentUsername(), null,
                action, "TAX_DECLARATION", String.valueOf(decl.getId()),
                messageKey, null,
                periodLabel(decl.getPeriodType(), decl.getPeriodYear(), decl.getPeriodNumber()));
    }

    private TaxRateCatalogDTO toCatalogDTO(TaxRateCatalog c) {
        return TaxRateCatalogDTO.builder()
                .code(c.getCode())
                .name(c.getName())
                .vatRate(c.getVatRate())
                .pitRate(c.getPitRate())
                .exemptThresholdYear(c.getExemptThresholdYear())
                .formThreshold(c.getFormThreshold())
                .build();
    }

    private TaxDeclarationDTO toDTO(TaxDeclaration d) {
        List<TaxDeclarationLineDTO> lines = d.getLines().stream()
                .map(l -> TaxDeclarationLineDTO.builder()
                        .id(l.getId())
                        .industryCode(l.getIndustryCode())
                        .industryName(l.getIndustryName())
                        .revenue(l.getRevenue())
                        .vatRate(l.getVatRate())
                        .pitRate(l.getPitRate())
                        .vatAmount(l.getVatAmount())
                        .pitAmount(l.getPitAmount())
                        .build())
                .collect(Collectors.toList());
        return TaxDeclarationDTO.builder()
                .id(d.getId())
                .periodType(d.getPeriodType().name())
                .periodYear(d.getPeriodYear())
                .periodNumber(d.getPeriodNumber())
                .periodLabel(periodLabel(d.getPeriodType(), d.getPeriodYear(), d.getPeriodNumber()))
                .businessType(d.getBusinessType() != null ? d.getBusinessType().name() : null)
                .formType(d.getFormType() != null ? d.getFormType().name() : null)
                .declaredRevenue(d.getDeclaredRevenue())
                .autoRevenue(d.getAutoRevenue())
                .totalVat(d.getTotalVat())
                .totalPit(d.getTotalPit())
                .totalTax(d.getTotalTax())
                .status(d.getStatus().name())
                .govRefNumber(d.getGovRefNumber())
                .submittedAt(d.getSubmittedAt())
                .notes(d.getNotes())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .lines(lines)
                .build();
    }
}

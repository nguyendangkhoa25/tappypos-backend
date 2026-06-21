package com.tappy.pos.service.report;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.dto.report.CashDrawerDTO;
import com.tappy.pos.model.dto.report.CloseDrawerRequest;
import com.tappy.pos.model.entity.finance.CashDrawerClose;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.CashDrawerCloseRepository;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashDrawerServiceImpl implements CashDrawerService {

    private final OrderRepository orderRepository;
    private final PawnRepository pawnRepository;
    private final ShopExpenseRepository shopExpenseRepository;
    private final CashDrawerCloseRepository cashDrawerCloseRepository;
    private final FeatureContext featureContext;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;
    private final AuthContext authContext;

    @Override
    @Transactional(readOnly = true)
    public CashDrawerDTO getReconciliation(LocalDate date) {
        final LocalDate day = date != null ? date : LocalDate.now();
        Optional<CashDrawerClose> existing = cashDrawerCloseRepository.findByBusinessDateAndDeletedFalse(day);
        BigDecimal opening = existing.map(CashDrawerClose::getOpeningAmount).orElseGet(() -> carryOverOpening(day));
        CashDrawerDTO dto = compute(day, opening);
        existing.ifPresent(c -> applyClose(dto, c));
        return dto;
    }

    @Override
    @Transactional
    public CashDrawerDTO close(CloseDrawerRequest request) {
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        BigDecimal opening = nz(request.getOpening());
        BigDecimal counted = nz(request.getCounted());

        CashDrawerDTO dto = compute(date, opening);
        BigDecimal difference = counted.subtract(dto.getExpected());

        CashDrawerClose entity = cashDrawerCloseRepository.findByBusinessDateAndDeletedFalse(date)
                .orElseGet(CashDrawerClose::new);
        entity.setTenantId(tenantContext.getCurrentTenantId());
        entity.setBusinessDate(date);
        entity.setOpeningAmount(opening);
        entity.setExpectedAmount(dto.getExpected());
        entity.setCountedAmount(counted);
        entity.setDifferenceAmount(difference);
        entity.setNote(request.getNote());
        entity.setClosedBy(currentUsername());
        entity.setClosedAt(LocalDateTime.now());
        cashDrawerCloseRepository.save(entity);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CASH_DRAWER_CLOSED, "CASH_DRAWER", String.valueOf(entity.getId()),
                "Chốt sổ quỹ ngày " + date, null);

        applyClose(dto, entity);
        return dto;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal carryOverOpening(LocalDate date) {
        return cashDrawerCloseRepository
                .findTopByBusinessDateLessThanAndDeletedFalseOrderByBusinessDateDesc(date)
                .map(CashDrawerClose::getCountedAmount)
                .orElse(BigDecimal.ZERO);
    }

    /** Live breakdown + expected cash for the day, using the supplied opening float. */
    private CashDrawerDTO compute(LocalDate date, BigDecimal opening) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        String tenantId = tenantContext.getCurrentTenantId();

        BigDecimal cashSales = nz(orderRepository.sumCashSalesByDateRange(from, to));
        BigDecimal goldBuy = nz(orderRepository.sumCashBuyByDateRange(from, to));
        BigDecimal cashExpenses = nz(shopExpenseRepository.sumCashByDate(tenantId, date));

        boolean pawnEnabled = featureContext.hasFeature("PAWN");
        BigDecimal pawnLoans = BigDecimal.ZERO;
        BigDecimal pawnRedeemed = BigDecimal.ZERO;
        if (pawnEnabled) {
            pawnLoans = nz(pawnRepository.sumNewPawnAmountByDateRange(from, to));
            List<Object[]> redeemRows = pawnRepository
                    .sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.REDEEMED), from, to, false);
            BigDecimal principal = (redeemRows.isEmpty() || redeemRows.get(0)[0] == null)
                    ? BigDecimal.ZERO : new BigDecimal(redeemRows.get(0)[0].toString());
            Long interestRaw = pawnRepository
                    .sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(List.of(PawnStatus.REDEEMED), from, to, false);
            pawnRedeemed = principal.add(interestRaw == null ? BigDecimal.ZERO : BigDecimal.valueOf(interestRaw));
        }

        BigDecimal expected = opening
                .add(cashSales).add(pawnRedeemed)
                .subtract(goldBuy).subtract(pawnLoans).subtract(cashExpenses);

        return CashDrawerDTO.builder()
                .businessDate(date)
                .opening(opening)
                .cashSales(cashSales)
                .pawnRedeemed(pawnRedeemed)
                .goldBuy(goldBuy)
                .pawnLoans(pawnLoans)
                .cashExpenses(cashExpenses)
                .expected(expected)
                .pawnEnabled(pawnEnabled)
                .closed(false)
                .build();
    }

    /** Overlay a saved close; difference is recomputed live against the (live) expected. */
    private void applyClose(CashDrawerDTO dto, CashDrawerClose c) {
        dto.setClosed(true);
        dto.setCounted(c.getCountedAmount());
        dto.setDifference(nz(c.getCountedAmount()).subtract(dto.getExpected()));
        dto.setClosedBy(c.getClosedBy());
        dto.setClosedAt(c.getClosedAt());
        dto.setNote(c.getNote());
    }

    private static String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}

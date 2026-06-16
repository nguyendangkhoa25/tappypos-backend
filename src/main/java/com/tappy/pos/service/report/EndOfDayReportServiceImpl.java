package com.tappy.pos.service.report;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.dto.report.EndOfDayReportDTO;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EndOfDayReportServiceImpl implements EndOfDayReportService {

    private final OrderRepository orderRepository;
    private final PawnRepository pawnRepository;
    private final FeatureContext featureContext;

    @Override
    public EndOfDayReportDTO getEndOfDay(LocalDate date) {
        // JVM default zone is Asia/Ho_Chi_Minh (set at startup), so LocalDate maps to the shop's day.
        if (date == null) date = LocalDate.now();
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        // ── Gold sold (cash in) — count/weight scoped to SELL to match the SELL ₫ ──
        BigDecimal soldAmount = nz(orderRepository.sumRevenueByDateRange(from, to));
        EndOfDayReportDTO.GoldLine goldSold = goldLine(orderRepository.goldSoldSummary(from, to), soldAmount);

        // ── Gold bought (cash out) — count/weight scoped to BUY/EXCHANGE to match buy_amount ──
        BigDecimal boughtAmount = nz(orderRepository.sumBuyAmountByDateRange(from, to));
        EndOfDayReportDTO.GoldLine goldBought = goldLine(orderRepository.goldBoughtSummary(from, to), boughtAmount);

        BigDecimal cashIn = soldAmount;
        BigDecimal cashOut = boughtAmount;

        // ── Pawn (only when the shop has the feature) ──
        boolean pawnEnabled = featureContext.hasFeature("PAWN");
        EndOfDayReportDTO.PawnNew pawnNew = null;
        EndOfDayReportDTO.PawnRedeemed pawnRedeemed = null;
        if (pawnEnabled) {
            BigDecimal newAmount = nz(pawnRepository.sumNewPawnAmountByDateRange(from, to));
            pawnNew = EndOfDayReportDTO.PawnNew.builder()
                    .count(nz(pawnRepository.countNewPawnsByDateRange(from, to)))
                    .amount(newAmount)
                    .build();

            // Redemptions today: principal returned + interest collected.
            List<Object[]> redeemRows = pawnRepository
                    .sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(
                            List.of(PawnStatus.REDEEMED), from, to, false);
            BigDecimal redeemPrincipal = (redeemRows.isEmpty() || redeemRows.get(0)[0] == null)
                    ? BigDecimal.ZERO : new BigDecimal(redeemRows.get(0)[0].toString());
            Long interestRaw = pawnRepository
                    .sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(
                            List.of(PawnStatus.REDEEMED), from, to, false);
            BigDecimal redeemInterest = interestRaw == null ? BigDecimal.ZERO : BigDecimal.valueOf(interestRaw);
            pawnRedeemed = EndOfDayReportDTO.PawnRedeemed.builder()
                    .amount(redeemPrincipal)
                    .interest(redeemInterest)
                    .build();

            cashOut = cashOut.add(newAmount);
            cashIn = cashIn.add(redeemPrincipal).add(redeemInterest);
        }

        return EndOfDayReportDTO.builder()
                .date(date)
                .goldSold(goldSold)
                .goldBought(goldBought)
                .pawnEnabled(pawnEnabled)
                .pawnNew(pawnNew)
                .pawnRedeemed(pawnRedeemed)
                .totals(EndOfDayReportDTO.Totals.builder()
                        .cashIn(cashIn).cashOut(cashOut).net(cashIn.subtract(cashOut)).build())
                .build();
    }

    /** Build a gold line from a [count, weight] aggregate row plus the (separately-summed) ₫ amount. */
    private static EndOfDayReportDTO.GoldLine goldLine(List<Object[]> rows, BigDecimal amount) {
        Object[] r = rows.isEmpty() ? null : rows.get(0);
        long count = (r == null || r[0] == null) ? 0L : ((Number) r[0]).longValue();
        BigDecimal weight = (r == null || r[1] == null) ? BigDecimal.ZERO : new BigDecimal(r[1].toString());
        return EndOfDayReportDTO.GoldLine.builder().count(count).weightChi(weight).amount(amount).build();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static long nz(Long v) { return v == null ? 0L : v; }
}

package com.knp.service;

import com.knp.model.dto.revenue.PaymentBreakdownDTO;
import com.knp.model.dto.revenue.RevenueOverviewDTO;
import com.knp.model.dto.revenue.RevenuePeriodDTO;
import com.knp.model.dto.revenue.TopProductDTO;
import com.knp.repository.OrderItemRepository;
import com.knp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueServiceImpl implements RevenueService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final String[] VI_MONTHS = {
        "Th1","Th2","Th3","Th4","Th5","Th6","Th7","Th8","Th9","Th10","Th11","Th12"
    };

    @Override
    public RevenueOverviewDTO getOverview() {
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        BigDecimal totalRevenue  = orderRepository.sumTotalRevenue();
        BigDecimal totalCost     = orderItemRepository.sumTotalCost();
        BigDecimal totalTax      = orderRepository.sumTotalTax();
        BigDecimal totalDiscount = orderRepository.sumTotalDiscount();
        Long       totalOrders   = orderRepository.countCompleted();

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal monthRevenue = orderRepository.sumRevenueByMonth(year, month);
        BigDecimal monthCost    = orderItemRepository.sumCostByMonth(year, month);
        Long       monthOrders  = orderRepository.countCompletedByMonth(year, month);

        BigDecimal yearRevenue = orderRepository.sumRevenueByYear(year);
        BigDecimal yearCost    = orderItemRepository.sumCostByYear(year);
        Long       yearOrders  = orderRepository.countCompletedByYear(year);

        return RevenueOverviewDTO.builder()
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .profitMarginPercent(profitMargin.setScale(1, RoundingMode.HALF_UP))
                .totalTax(totalTax)
                .totalDiscount(totalDiscount)
                .totalOrders(totalOrders)
                .avgOrderValue(avgOrderValue)
                .monthRevenue(monthRevenue)
                .monthCost(monthCost)
                .monthProfit(monthRevenue.subtract(monthCost))
                .monthOrders(monthOrders)
                .yearRevenue(yearRevenue)
                .yearCost(yearCost)
                .yearProfit(yearRevenue.subtract(yearCost))
                .yearOrders(yearOrders)
                .currentMonth(month)
                .currentYear(year)
                .build();
    }

    @Override
    public List<RevenuePeriodDTO> getMonthlyBreakdown(int year) {
        List<Object[]> revenueRows = orderRepository.sumRevenueGroupedByMonth(year);
        List<Object[]> costRows    = orderItemRepository.sumCostGroupedByMonth(year);

        Map<Integer, BigDecimal> costByMonth = new HashMap<>();
        for (Object[] row : costRows) {
            costByMonth.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        }

        Map<Integer, Object[]> revenueByMonth = new LinkedHashMap<>();
        for (Object[] row : revenueRows) {
            revenueByMonth.put(((Number) row[0]).intValue(), row);
        }

        List<RevenuePeriodDTO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Object[]   row     = revenueByMonth.get(m);
            BigDecimal revenue = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long       count   = row != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal cost    = costByMonth.getOrDefault(m, BigDecimal.ZERO);
            result.add(RevenuePeriodDTO.builder()
                    .label(VI_MONTHS[m - 1])
                    .period(m)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(revenue.subtract(cost))
                    .orderCount(count)
                    .build());
        }
        return result;
    }

    @Override
    public List<RevenuePeriodDTO> getDailyBreakdown(int year, int month) {
        List<Object[]> revenueRows = orderRepository.sumRevenueGroupedByDay(year, month);
        List<Object[]> costRows    = orderItemRepository.sumCostGroupedByDay(year, month);

        Map<Integer, BigDecimal> costByDay = new HashMap<>();
        for (Object[] row : costRows) {
            costByDay.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        }
        Map<Integer, Object[]> revenueByDay = new LinkedHashMap<>();
        for (Object[] row : revenueRows) {
            revenueByDay.put(((Number) row[0]).intValue(), row);
        }

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        List<RevenuePeriodDTO> result = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            Object[]   row     = revenueByDay.get(d);
            BigDecimal revenue = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long       count   = row != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal cost    = costByDay.getOrDefault(d, BigDecimal.ZERO);
            result.add(RevenuePeriodDTO.builder()
                    .label(String.format("%02d/%02d", d, month))
                    .period(d)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(revenue.subtract(cost))
                    .orderCount(count)
                    .build());
        }
        return result;
    }

    @Override
    public List<TopProductDTO> getTopProducts(Integer year, Integer month, int limit) {
        List<Object[]> rows = orderItemRepository.findTopProducts(year, month);
        return rows.stream()
                .limit(limit)
                .map(row -> TopProductDTO.builder()
                        .productId(row[0] != null ? ((Number) row[0]).longValue() : null)
                        .productName((String) row[1])
                        .quantitySold(((Number) row[2]).longValue())
                        .revenue((BigDecimal) row[3])
                        .cost((BigDecimal) row[4])
                        .profit(((BigDecimal) row[3]).subtract((BigDecimal) row[4]))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentBreakdownDTO> getPaymentBreakdown(Integer year, Integer month) {
        List<Object[]> rows = orderRepository.groupByPaymentMethod(year, month);

        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    BigDecimal amount = (BigDecimal) row[2];
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return PaymentBreakdownDTO.builder()
                            .paymentMethod(row[0] != null ? (String) row[0] : "UNKNOWN")
                            .orderCount(((Number) row[1]).longValue())
                            .totalAmount(amount)
                            .percentage(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }
}

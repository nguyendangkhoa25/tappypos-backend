package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.revenue.CategoryRevenueDTO;
import com.tappy.pos.model.dto.revenue.DayOfWeekRevenueDTO;
import com.tappy.pos.model.dto.revenue.HourlyRevenueDTO;
import com.tappy.pos.model.dto.revenue.PaymentBreakdownDTO;
import com.tappy.pos.model.dto.revenue.RevenueOverviewDTO;
import com.tappy.pos.model.dto.revenue.RevenuePeriodDTO;
import com.tappy.pos.model.dto.revenue.TopEmployeeDTO;
import com.tappy.pos.model.dto.revenue.TopProductDTO;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
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
    private final ShopExpenseRepository expenseRepository;

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

        BigDecimal totalExpenses = expenseRepository.sumAll();
        BigDecimal monthExpenses = expenseRepository.sumByMonth(year, month);
        BigDecimal yearExpenses  = expenseRepository.sumByYear(year);

        BigDecimal monthGrossProfit = monthRevenue.subtract(monthCost);
        BigDecimal yearGrossProfit  = yearRevenue.subtract(yearCost);

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
                .monthProfit(monthGrossProfit)
                .monthOrders(monthOrders)
                .yearRevenue(yearRevenue)
                .yearCost(yearCost)
                .yearProfit(yearGrossProfit)
                .yearOrders(yearOrders)
                .totalExpenses(totalExpenses)
                .monthExpenses(monthExpenses)
                .yearExpenses(yearExpenses)
                .monthNetProfit(monthGrossProfit.subtract(monthExpenses))
                .yearNetProfit(yearGrossProfit.subtract(yearExpenses))
                .currentMonth(month)
                .currentYear(year)
                .build();
    }

    @Override
    public List<RevenuePeriodDTO> getMonthlyBreakdown(int year) {
        List<Object[]> revenueRows = orderRepository.sumRevenueGroupedByMonth(year);
        List<Object[]> costRows    = orderItemRepository.sumCostGroupedByMonth(year);
        List<Object[]> expenseRows = expenseRepository.sumGroupedByMonth(year);

        Map<Integer, BigDecimal> costByMonth    = new HashMap<>();
        Map<Integer, BigDecimal> expenseByMonth = new HashMap<>();
        for (Object[] row : costRows)    costByMonth.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        for (Object[] row : expenseRows) expenseByMonth.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);

        Map<Integer, Object[]> revenueByMonth = new LinkedHashMap<>();
        for (Object[] row : revenueRows) {
            revenueByMonth.put(((Number) row[0]).intValue(), row);
        }

        List<RevenuePeriodDTO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Object[]   row      = revenueByMonth.get(m);
            BigDecimal revenue  = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long       count    = row != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal cost     = costByMonth.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal expenses = expenseByMonth.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal profit   = revenue.subtract(cost);
            result.add(RevenuePeriodDTO.builder()
                    .label(VI_MONTHS[m - 1])
                    .period(m)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(profit)
                    .orderCount(count)
                    .expenses(expenses)
                    .netProfit(profit.subtract(expenses))
                    .build());
        }
        return result;
    }

    @Override
    public List<RevenuePeriodDTO> getDailyBreakdown(int year, int month) {
        List<Object[]> revenueRows = orderRepository.sumRevenueGroupedByDay(year, month);
        List<Object[]> costRows    = orderItemRepository.sumCostGroupedByDay(year, month);
        List<Object[]> expenseRows = expenseRepository.sumGroupedByDay(year, month);

        Map<Integer, BigDecimal> costByDay    = new HashMap<>();
        Map<Integer, BigDecimal> expenseByDay = new HashMap<>();
        for (Object[] row : costRows)    costByDay.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        for (Object[] row : expenseRows) expenseByDay.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);

        Map<Integer, Object[]> revenueByDay = new LinkedHashMap<>();
        for (Object[] row : revenueRows) {
            revenueByDay.put(((Number) row[0]).intValue(), row);
        }

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        List<RevenuePeriodDTO> result = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            Object[]   row      = revenueByDay.get(d);
            BigDecimal revenue  = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long       count    = row != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal cost     = costByDay.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal expenses = expenseByDay.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal profit   = revenue.subtract(cost);
            result.add(RevenuePeriodDTO.builder()
                    .label(String.format("%02d/%02d", d, month))
                    .period(d)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(profit)
                    .orderCount(count)
                    .expenses(expenses)
                    .netProfit(profit.subtract(expenses))
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

    @Override
    public List<DayOfWeekRevenueDTO> getDayOfWeekBreakdown(Integer year, Integer month) {
        List<Object[]> rows = orderRepository.sumRevenueGroupedByDayOfWeek(year, month);
        Map<Integer, Object[]> byDay = new HashMap<>();
        for (Object[] row : rows) byDay.put(((Number) row[0]).intValue(), row);

        List<DayOfWeekRevenueDTO> result = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            Object[] row = byDay.get(d);
            result.add(DayOfWeekRevenueDTO.builder()
                    .dayOfWeek(d)
                    .revenue(row != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                    .orderCount(row != null ? ((Number) row[2]).longValue() : 0L)
                    .build());
        }
        return result;
    }

    @Override
    public List<HourlyRevenueDTO> getHourlyBreakdown(Integer year, Integer month) {
        List<Object[]> rows = orderRepository.sumRevenueGroupedByHour(year, month);
        Map<Integer, Object[]> byHour = new HashMap<>();
        for (Object[] row : rows) byHour.put(((Number) row[0]).intValue(), row);

        List<HourlyRevenueDTO> result = new ArrayList<>();
        for (int h = 0; h <= 23; h++) {
            Object[] row = byHour.get(h);
            result.add(HourlyRevenueDTO.builder()
                    .hour(h)
                    .revenue(row != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                    .orderCount(row != null ? ((Number) row[2]).longValue() : 0L)
                    .build());
        }
        return result;
    }

    @Override
    public List<CategoryRevenueDTO> getCategoryBreakdown(Integer year, Integer month) {
        List<Object[]> rows = orderRepository.sumRevenueGroupedByCategory(year, month);

        BigDecimal grandTotal = rows.stream()
                .map(r -> toBigDecimal(r[2]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    BigDecimal revenue = toBigDecimal(row[2]);
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                            ? revenue.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return CategoryRevenueDTO.builder()
                            .categoryName((String) row[0])
                            .orderCount(((Number) row[1]).longValue())
                            .revenue(revenue)
                            .percentage(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TopEmployeeDTO> getTopEmployees(Integer year, Integer month, int limit) {
        List<Object[]> rows = orderRepository.sumRevenueGroupedByEmployee(year, month);
        return rows.stream()
                .limit(limit)
                .map(row -> TopEmployeeDTO.builder()
                        .employeeName((String) row[0])
                        .orderCount(((Number) row[1]).longValue())
                        .revenue(toBigDecimal(row[2]))
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }
}

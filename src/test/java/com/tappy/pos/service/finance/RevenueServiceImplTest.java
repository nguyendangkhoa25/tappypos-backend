package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.revenue.*;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueServiceImpl Unit Tests")
class RevenueServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShopExpenseRepository expenseRepository;

    @InjectMocks
    private RevenueServiceImpl revenueService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @BeforeEach
    void stubDefaults() {
        lenient().when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("1000000"));
        lenient().when(orderItemRepository.sumTotalCost()).thenReturn(new BigDecimal("600000"));
        lenient().when(orderRepository.sumTotalTax()).thenReturn(new BigDecimal("50000"));
        lenient().when(orderRepository.sumTotalDiscount()).thenReturn(new BigDecimal("20000"));
        lenient().when(orderRepository.countCompleted()).thenReturn(100L);
        lenient().when(orderRepository.sumRevenueByMonth(anyInt(), anyInt())).thenReturn(new BigDecimal("100000"));
        lenient().when(orderItemRepository.sumCostByMonth(anyInt(), anyInt())).thenReturn(new BigDecimal("60000"));
        lenient().when(orderRepository.countCompletedByMonth(anyInt(), anyInt())).thenReturn(10L);
        lenient().when(orderRepository.sumRevenueByYear(anyInt())).thenReturn(new BigDecimal("1200000"));
        lenient().when(orderItemRepository.sumCostByYear(anyInt())).thenReturn(new BigDecimal("720000"));
        lenient().when(orderRepository.countCompletedByYear(anyInt())).thenReturn(120L);
        lenient().when(expenseRepository.sumAll()).thenReturn(new BigDecimal("50000"));
        lenient().when(expenseRepository.sumByMonth(anyInt(), anyInt())).thenReturn(new BigDecimal("5000"));
        lenient().when(expenseRepository.sumByYear(anyInt())).thenReturn(new BigDecimal("60000"));
    }

    // ── getOverview ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOverview returns correct totals")
    void getOverview_returnsTotals() {
        RevenueOverviewDTO dto = revenueService.getOverview();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalRevenue()).isEqualByComparingTo("1000000");
        assertThat(dto.getTotalCost()).isEqualByComparingTo("600000");
        assertThat(dto.getGrossProfit()).isEqualByComparingTo("400000");
        assertThat(dto.getTotalOrders()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getOverview computes profit margin correctly")
    void getOverview_profitMargin() {
        RevenueOverviewDTO dto = revenueService.getOverview();
        // 400000 / 1000000 = 40.0%
        assertThat(dto.getProfitMarginPercent()).isEqualByComparingTo("40.0");
    }

    @Test
    @DisplayName("getOverview computes avg order value correctly")
    void getOverview_avgOrderValue() {
        RevenueOverviewDTO dto = revenueService.getOverview();
        // 1000000 / 100 = 10000
        assertThat(dto.getAvgOrderValue()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("getOverview returns zero profit margin when revenue is zero")
    void getOverview_zeroProfitMarginWhenNoRevenue() {
        when(orderRepository.sumTotalRevenue()).thenReturn(ZERO);
        when(orderItemRepository.sumTotalCost()).thenReturn(ZERO);
        when(orderRepository.countCompleted()).thenReturn(0L);

        RevenueOverviewDTO dto = revenueService.getOverview();
        assertThat(dto.getProfitMarginPercent()).isEqualByComparingTo("0");
        assertThat(dto.getAvgOrderValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getOverview populates month and year fields")
    void getOverview_monthAndYearFields() {
        RevenueOverviewDTO dto = revenueService.getOverview();
        assertThat(dto.getMonthRevenue()).isEqualByComparingTo("100000");
        assertThat(dto.getYearRevenue()).isEqualByComparingTo("1200000");
        assertThat(dto.getCurrentMonth()).isGreaterThan(0);
        assertThat(dto.getCurrentYear()).isGreaterThan(2000);
    }

    @Test
    @DisplayName("getOverview computes net profit correctly")
    void getOverview_netProfit() {
        RevenueOverviewDTO dto = revenueService.getOverview();
        // monthNetProfit = monthRevenue - monthCost - monthExpenses = 100000 - 60000 - 5000 = 35000
        assertThat(dto.getMonthNetProfit()).isEqualByComparingTo("35000");
    }

    // ── getMonthlyBreakdown ───────────────────────────────────────────────────

    @Test
    @DisplayName("getMonthlyBreakdown returns 12 entries")
    void getMonthlyBreakdown_returns12Months() {
        when(orderRepository.sumRevenueGroupedByMonth(2024)).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumCostGroupedByMonth(2024)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByMonth(2024)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getMonthlyBreakdown(2024);

        assertThat(result).hasSize(12);
        assertThat(result.get(0).getLabel()).isEqualTo("Th1");
        assertThat(result.get(11).getLabel()).isEqualTo("Th12");
    }

    @Test
    @DisplayName("getMonthlyBreakdown fills zeros for months with no orders")
    void getMonthlyBreakdown_zeroForMissingMonths() {
        when(orderRepository.sumRevenueGroupedByMonth(2024)).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumCostGroupedByMonth(2024)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByMonth(2024)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getMonthlyBreakdown(2024);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(ZERO);
        assertThat(result.get(0).getOrderCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getMonthlyBreakdown maps revenue row correctly")
    void getMonthlyBreakdown_mapsRow() {
        List<Object[]> revenueRows = new ArrayList<>();
        revenueRows.add(new Object[]{ 3, new BigDecimal("500000"), 50L });
        when(orderRepository.sumRevenueGroupedByMonth(2024)).thenReturn(revenueRows);
        when(orderItemRepository.sumCostGroupedByMonth(2024)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByMonth(2024)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getMonthlyBreakdown(2024);

        RevenuePeriodDTO march = result.get(2);
        assertThat(march.getRevenue()).isEqualByComparingTo("500000");
        assertThat(march.getOrderCount()).isEqualTo(50L);
        assertThat(march.getLabel()).isEqualTo("Th3");
    }

    // ── getDailyBreakdown ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getDailyBreakdown returns correct days for February non-leap year")
    void getDailyBreakdown_februaryNonLeap() {
        when(orderRepository.sumRevenueGroupedByDay(2023, 2)).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumCostGroupedByDay(2023, 2)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByDay(2023, 2)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getDailyBreakdown(2023, 2);

        assertThat(result).hasSize(28);
        assertThat(result.get(0).getLabel()).isEqualTo("01/02");
    }

    @Test
    @DisplayName("getDailyBreakdown returns 31 days for January")
    void getDailyBreakdown_january() {
        when(orderRepository.sumRevenueGroupedByDay(2024, 1)).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumCostGroupedByDay(2024, 1)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByDay(2024, 1)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getDailyBreakdown(2024, 1);

        assertThat(result).hasSize(31);
    }

    @Test
    @DisplayName("getDailyBreakdown maps a data row correctly")
    void getDailyBreakdown_mapsRow() {
        List<Object[]> revenueRows = new ArrayList<>();
        revenueRows.add(new Object[]{ 15, new BigDecimal("200000"), 20L });
        when(orderRepository.sumRevenueGroupedByDay(2024, 5)).thenReturn(revenueRows);
        when(orderItemRepository.sumCostGroupedByDay(2024, 5)).thenReturn(Collections.emptyList());
        when(expenseRepository.sumGroupedByDay(2024, 5)).thenReturn(Collections.emptyList());

        List<RevenuePeriodDTO> result = revenueService.getDailyBreakdown(2024, 5);

        RevenuePeriodDTO day15 = result.get(14);
        assertThat(day15.getRevenue()).isEqualByComparingTo("200000");
        assertThat(day15.getLabel()).isEqualTo("15/05");
    }

    // ── getTopProducts ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopProducts returns empty list when no data")
    void getTopProducts_emptyList() {
        when(orderItemRepository.findTopProducts(null, null)).thenReturn(Collections.emptyList());

        List<TopProductDTO> result = revenueService.getTopProducts(null, null, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopProducts respects limit")
    void getTopProducts_limitsResults() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Product A", 100L, new BigDecimal("500000"), new BigDecimal("300000")});
        rows.add(new Object[]{2L, "Product B", 80L,  new BigDecimal("400000"), new BigDecimal("240000")});
        rows.add(new Object[]{3L, "Product C", 60L,  new BigDecimal("300000"), new BigDecimal("180000")});
        when(orderItemRepository.findTopProducts(2024, 5)).thenReturn(rows);

        List<TopProductDTO> result = revenueService.getTopProducts(2024, 5, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("Product A");
    }

    @Test
    @DisplayName("getTopProducts computes profit correctly")
    void getTopProducts_computesProfit() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Cà Phê", 50L, new BigDecimal("250000"), new BigDecimal("100000")});
        when(orderItemRepository.findTopProducts(null, null)).thenReturn(rows);

        List<TopProductDTO> result = revenueService.getTopProducts(null, null, 10);

        assertThat(result.get(0).getProfit()).isEqualByComparingTo("150000");
    }

    // ── getPaymentBreakdown ───────────────────────────────────────────────────

    @Test
    @DisplayName("getPaymentBreakdown returns empty list when no orders")
    void getPaymentBreakdown_empty() {
        when(orderRepository.groupByPaymentMethod(null, null)).thenReturn(Collections.emptyList());

        List<PaymentBreakdownDTO> result = revenueService.getPaymentBreakdown(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPaymentBreakdown computes percentage correctly")
    void getPaymentBreakdown_percentage() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"CASH", 80L, new BigDecimal("800000")});
        rows.add(new Object[]{"CARD", 20L, new BigDecimal("200000")});
        when(orderRepository.groupByPaymentMethod(null, null)).thenReturn(rows);

        List<PaymentBreakdownDTO> result = revenueService.getPaymentBreakdown(null, null);

        assertThat(result).hasSize(2);
        PaymentBreakdownDTO cash = result.get(0);
        assertThat(cash.getPaymentMethod()).isEqualTo("CASH");
        assertThat(cash.getPercentage()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("getPaymentBreakdown uses UNKNOWN for null payment method")
    void getPaymentBreakdown_nullPaymentMethod() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{null, 10L, new BigDecimal("100000")});
        when(orderRepository.groupByPaymentMethod(null, null)).thenReturn(rows);

        List<PaymentBreakdownDTO> result = revenueService.getPaymentBreakdown(null, null);

        assertThat(result.get(0).getPaymentMethod()).isEqualTo("UNKNOWN");
    }

    // ── getDayOfWeekBreakdown ─────────────────────────────────────────────────

    @Test
    @DisplayName("getDayOfWeekBreakdown returns 7 entries")
    void getDayOfWeekBreakdown_returns7Days() {
        when(orderRepository.sumRevenueGroupedByDayOfWeek(null, null)).thenReturn(Collections.emptyList());

        List<DayOfWeekRevenueDTO> result = revenueService.getDayOfWeekBreakdown(null, null);

        assertThat(result).hasSize(7);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo(1);
        assertThat(result.get(6).getDayOfWeek()).isEqualTo(7);
    }

    @Test
    @DisplayName("getDayOfWeekBreakdown fills zeros for missing days")
    void getDayOfWeekBreakdown_zeroForMissingDays() {
        when(orderRepository.sumRevenueGroupedByDayOfWeek(null, null)).thenReturn(Collections.emptyList());

        List<DayOfWeekRevenueDTO> result = revenueService.getDayOfWeekBreakdown(null, null);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(ZERO);
        assertThat(result.get(0).getOrderCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getDayOfWeekBreakdown maps data row")
    void getDayOfWeekBreakdown_mapsRow() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{ 3, new BigDecimal("150000"), 15L });
        when(orderRepository.sumRevenueGroupedByDayOfWeek(2024, null)).thenReturn(rows);

        List<DayOfWeekRevenueDTO> result = revenueService.getDayOfWeekBreakdown(2024, null);

        DayOfWeekRevenueDTO wed = result.get(2);
        assertThat(wed.getRevenue()).isEqualByComparingTo("150000");
        assertThat(wed.getOrderCount()).isEqualTo(15L);
    }

    // ── getHourlyBreakdown ────────────────────────────────────────────────────

    @Test
    @DisplayName("getHourlyBreakdown returns 24 entries")
    void getHourlyBreakdown_returns24Hours() {
        when(orderRepository.sumRevenueGroupedByHour(null, null)).thenReturn(Collections.emptyList());

        List<HourlyRevenueDTO> result = revenueService.getHourlyBreakdown(null, null);

        assertThat(result).hasSize(24);
        assertThat(result.get(0).getHour()).isEqualTo(0);
        assertThat(result.get(23).getHour()).isEqualTo(23);
    }

    @Test
    @DisplayName("getHourlyBreakdown maps a data row correctly")
    void getHourlyBreakdown_mapsRow() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{ 9, new BigDecimal("300000"), 30L });
        when(orderRepository.sumRevenueGroupedByHour(2024, 5)).thenReturn(rows);

        List<HourlyRevenueDTO> result = revenueService.getHourlyBreakdown(2024, 5);

        HourlyRevenueDTO hour9 = result.get(9);
        assertThat(hour9.getRevenue()).isEqualByComparingTo("300000");
        assertThat(hour9.getOrderCount()).isEqualTo(30L);
    }

    // ── getCategoryBreakdown ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryBreakdown returns empty list")
    void getCategoryBreakdown_empty() {
        when(orderRepository.sumRevenueGroupedByCategory(null, null)).thenReturn(Collections.emptyList());

        List<CategoryRevenueDTO> result = revenueService.getCategoryBreakdown(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryBreakdown computes category percentage")
    void getCategoryBreakdown_percentage() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Đồ uống", 40L, new BigDecimal("600000")});
        rows.add(new Object[]{"Thực phẩm", 20L, new BigDecimal("400000")});
        when(orderRepository.sumRevenueGroupedByCategory(null, null)).thenReturn(rows);

        List<CategoryRevenueDTO> result = revenueService.getCategoryBreakdown(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Đồ uống");
        assertThat(result.get(0).getPercentage()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("getCategoryBreakdown handles null revenue value via toBigDecimal")
    void getCategoryBreakdown_nullRevenue() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Khác", 5L, null});
        when(orderRepository.sumRevenueGroupedByCategory(null, null)).thenReturn(rows);

        List<CategoryRevenueDTO> result = revenueService.getCategoryBreakdown(null, null);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(ZERO);
        assertThat(result.get(0).getPercentage()).isEqualTo(0.0);
    }

    // ── getTopEmployees ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopEmployees returns empty list")
    void getTopEmployees_empty() {
        when(orderRepository.sumRevenueGroupedByEmployee(null, null)).thenReturn(Collections.emptyList());

        List<TopEmployeeDTO> result = revenueService.getTopEmployees(null, null, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopEmployees respects limit")
    void getTopEmployees_limitsResults() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Nguyễn Văn A", 50L, new BigDecimal("500000")});
        rows.add(new Object[]{"Trần Thị B",   40L, new BigDecimal("400000")});
        rows.add(new Object[]{"Lê Văn C",      30L, new BigDecimal("300000")});
        when(orderRepository.sumRevenueGroupedByEmployee(2024, null)).thenReturn(rows);

        List<TopEmployeeDTO> result = revenueService.getTopEmployees(2024, null, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmployeeName()).isEqualTo("Nguyễn Văn A");
    }

    // ── getMonthlyBreakdown with populated cost/expense rows ──────────────────

    @Test
    @DisplayName("getMonthlyBreakdown maps cost and expense rows when data is present")
    void getMonthlyBreakdown_withCostAndExpenses() {
        List<Object[]> revenueRows = new ArrayList<>();
        revenueRows.add(new Object[]{ 5, new BigDecimal("500000"), 50L });

        List<Object[]> costRows = new ArrayList<>();
        costRows.add(new Object[]{ 5, new BigDecimal("300000") });

        List<Object[]> expenseRows = new ArrayList<>();
        expenseRows.add(new Object[]{ 5, new BigDecimal("20000") });

        when(orderRepository.sumRevenueGroupedByMonth(2024)).thenReturn(revenueRows);
        when(orderItemRepository.sumCostGroupedByMonth(2024)).thenReturn(costRows);
        when(expenseRepository.sumGroupedByMonth(2024)).thenReturn(expenseRows);

        List<RevenuePeriodDTO> result = revenueService.getMonthlyBreakdown(2024);

        RevenuePeriodDTO may = result.get(4);
        assertThat(may.getCost()).isEqualByComparingTo("300000");
        assertThat(may.getExpenses()).isEqualByComparingTo("20000");
        // profit = 500000 - 300000 = 200000; netProfit = 200000 - 20000 = 180000
        assertThat(may.getProfit()).isEqualByComparingTo("200000");
        assertThat(may.getNetProfit()).isEqualByComparingTo("180000");
    }

    // ── getDailyBreakdown with populated cost/expense rows ────────────────────

    @Test
    @DisplayName("getDailyBreakdown maps cost and expense rows when data is present")
    void getDailyBreakdown_withCostAndExpenses() {
        List<Object[]> revenueRows = new ArrayList<>();
        revenueRows.add(new Object[]{ 10, new BigDecimal("100000"), 10L });

        List<Object[]> costRows = new ArrayList<>();
        costRows.add(new Object[]{ 10, new BigDecimal("60000") });

        List<Object[]> expenseRows = new ArrayList<>();
        expenseRows.add(new Object[]{ 10, new BigDecimal("5000") });

        when(orderRepository.sumRevenueGroupedByDay(2024, 6)).thenReturn(revenueRows);
        when(orderItemRepository.sumCostGroupedByDay(2024, 6)).thenReturn(costRows);
        when(expenseRepository.sumGroupedByDay(2024, 6)).thenReturn(expenseRows);

        List<RevenuePeriodDTO> result = revenueService.getDailyBreakdown(2024, 6);

        RevenuePeriodDTO day10 = result.get(9);
        assertThat(day10.getCost()).isEqualByComparingTo("60000");
        assertThat(day10.getExpenses()).isEqualByComparingTo("5000");
        assertThat(day10.getProfit()).isEqualByComparingTo("40000");
        assertThat(day10.getNetProfit()).isEqualByComparingTo("35000");
    }

    // ── toBigDecimal: non-BigDecimal branch ───────────────────────────────────

    @Test
    @DisplayName("getCategoryBreakdown handles Long revenue value via toBigDecimal (non-BigDecimal path)")
    void getCategoryBreakdown_longRevenue_convertsViaToBigDecimal() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Electronics", 10L, 300000L}); // Long, not BigDecimal
        when(orderRepository.sumRevenueGroupedByCategory(null, null)).thenReturn(rows);

        List<CategoryRevenueDTO> result = revenueService.getCategoryBreakdown(null, null);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("getTopEmployees handles Long revenue value via toBigDecimal")
    void getTopEmployees_longRevenue_convertsViaToBigDecimal() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Nguyễn Văn A", 50L, 500000L}); // Long, not BigDecimal
        when(orderRepository.sumRevenueGroupedByEmployee(null, null)).thenReturn(rows);

        List<TopEmployeeDTO> result = revenueService.getTopEmployees(null, null, 5);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo("500000");
    }
}

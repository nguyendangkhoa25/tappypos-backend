package com.tappy.pos.service.finance;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseRequest;
import com.tappy.pos.model.entity.finance.ShopExpense;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.ExpenseCategory;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopExpenseServiceImpl Unit Tests")
class ShopExpenseServiceImplTest {

    @Mock private TenantContext tenantContext;
    @Mock private ShopExpenseRepository expenseRepository;
    @Mock private AuthContext authContext;
    @Mock private com.tappy.pos.service.audit.ActivityLogService activityLogService;
    @Mock private com.tappy.pos.service.MessageService messageService;

    @InjectMocks
    private ShopExpenseServiceImpl service;

    private ShopExpense expense;
    private ShopExpenseRequest request;

    @BeforeEach
    void setUp() {
        expense = ShopExpense.builder()
                .amount(new BigDecimal("500000"))
                .category(ExpenseCategory.RENT)
                .description("Monthly rent")
                .expenseDate(LocalDate.now())
                .build();

        request = new ShopExpenseRequest();
        request.setAmount(new BigDecimal("500000"));
        request.setCategory(ExpenseCategory.RENT);
        request.setDescription("Monthly rent");
        request.setExpenseDate(LocalDate.now());

        lenient().when(authContext.getCurrentUsername()).thenReturn("user1");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("test-tenant");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves expense with current user as creator")
    void create_success() {
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ShopExpenseDTO dto = service.create(request);

        ArgumentCaptor<ShopExpense> cap = ArgumentCaptor.forClass(ShopExpense.class);
        verify(expenseRepository).save(cap.capture());
        assertThat(cap.getValue().getCreatedBy()).isEqualTo("user1");
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: updates all fields")
    void update_success() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        request.setAmount(new BigDecimal("600000"));
        request.setDescription("Updated rent");

        ShopExpenseDTO dto = service.update(1L, request);

        assertThat(expense.getAmount()).isEqualByComparingTo(new BigDecimal("600000"));
        assertThat(expense.getDescription()).isEqualTo("Updated rent");
        assertThat(expense.getUpdatedBy()).isEqualTo("user1");
    }

    @Test
    @DisplayName("update: throws when expense not found")
    void update_notFound() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes expense")
    void delete_success() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenReturn(expense);

        service.delete(1L);

        assertThat(expense.isDeleted()).isTrue();
    }

    // ── getCategoryBreakdown ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryBreakdown: calculates correct percentages")
    void getCategoryBreakdown_calculatesPercentages() {
        Object[] row1 = {ExpenseCategory.RENT, new BigDecimal("600000")};
        Object[] row2 = {ExpenseCategory.ELECTRICITY, new BigDecimal("400000")};
        List<Object[]> rows = Arrays.asList(row1, row2);
        when(expenseRepository.sumGroupedByCategory("test-tenant", 2024, 1)).thenReturn(rows);

        List<ExpenseCategoryBreakdownDTO> result = service.getCategoryBreakdown(2024, 1);

        assertThat(result).hasSize(2);
        // RENT: 600000 / 1000000 = 60%
        assertThat(result.get(0).getCategory()).isEqualTo(ExpenseCategory.RENT);
        assertThat(result.get(0).getPercentage()).isEqualTo(60.0);
        // UTILITIES: 400000 / 1000000 = 40%
        assertThat(result.get(1).getPercentage()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("getCategoryBreakdown: returns 0% for all when total is zero")
    void getCategoryBreakdown_zeroTotal() {
        Object[] row = {ExpenseCategory.RENT, BigDecimal.ZERO};
        when(expenseRepository.sumGroupedByCategory("test-tenant", 2024, 1)).thenReturn(Collections.singletonList(row));

        List<ExpenseCategoryBreakdownDTO> result = service.getCategoryBreakdown(2024, 1);

        assertThat(result.get(0).getPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCategoryBreakdown: returns empty list when no data")
    void getCategoryBreakdown_empty() {
        when(expenseRepository.sumGroupedByCategory("test-tenant", 2024, 1)).thenReturn(List.of());

        assertThat(service.getCategoryBreakdown(2024, 1)).isEmpty();
    }

    // ── create / activity logging ──────────────────────────────────────────────

    @Test
    @DisplayName("create: logs EXPENSE_CREATED activity and returns mapped DTO")
    void create_logsActivity() {
        when(expenseRepository.save(any())).thenAnswer(i -> {
            ShopExpense e = i.getArgument(0);
            e.setId(7L);
            return e;
        });

        ShopExpenseDTO dto = service.create(request);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getCategoryDisplayName()).isEqualTo(ExpenseCategory.RENT.getDisplayName());
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), isNull(),
                eq(ActivityAction.EXPENSE_CREATED), eq("EXPENSE"), eq("7"),
                eq("activity.expense.created"), isNull(),
                eq("Monthly rent"), eq(new BigDecimal("500000")));
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns mapped DTO for active expense")
    void getById_success() {
        expense.setId(1L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ShopExpenseDTO dto = service.getById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("getById: throws when expense is soft-deleted")
    void getById_deletedTreatedAsNotFound() {
        expense.softDelete();
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: throws when expense not found")
    void getById_notFound() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update / delete activity logging ───────────────────────────────────────

    @Test
    @DisplayName("update: logs EXPENSE_UPDATED activity")
    void update_logsActivity() {
        expense.setId(1L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update(1L, request);

        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), isNull(),
                eq(ActivityAction.EXPENSE_UPDATED), eq("EXPENSE"), eq("1"),
                eq("activity.expense.updated"), isNull(), any(), any());
    }

    @Test
    @DisplayName("delete: logs EXPENSE_DELETED activity")
    void delete_logsActivity() {
        expense.setId(1L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenReturn(expense);

        service.delete(1L);

        assertThat(expense.isDeleted()).isTrue();
        verify(activityLogService).logAsync(eq("test-tenant"), eq("user1"), isNull(),
                eq(ActivityAction.EXPENSE_DELETED), eq("EXPENSE"), eq("1"),
                eq("activity.expense.deleted"), isNull(), any(), any());
    }

    @Test
    @DisplayName("delete: throws when expense not found")
    void delete_notFound() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── search ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: passes category name and maps page entries to DTOs")
    void search_withCategory() {
        expense.setId(1L);
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        Pageable pageable = PageRequest.of(0, 10);
        when(expenseRepository.search("test-tenant", from, to, "RENT", pageable))
                .thenReturn(new PageImpl<>(List.of(expense)));

        Page<ShopExpenseDTO> page = service.search(from, to, ExpenseCategory.RENT, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("search: passes null category when none provided")
    void search_nullCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        when(expenseRepository.search("test-tenant", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ShopExpenseDTO> page = service.search(null, null, null, pageable);

        assertThat(page.getContent()).isEmpty();
        verify(expenseRepository).search("test-tenant", null, null, null, pageable);
    }

    // ── getCategoryBreakdown(LocalDate, LocalDate) ─────────────────────────────

    @Test
    @DisplayName("getCategoryBreakdown(date range): parses category from string and computes percentages")
    void getCategoryBreakdown_dateRange() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        Object[] row1 = {"RENT", new BigDecimal("750000")};
        Object[] row2 = {"ELECTRICITY", new BigDecimal("250000")};
        when(expenseRepository.sumGroupedByCategoryDateRange("test-tenant", from, to))
                .thenReturn(Arrays.asList(row1, row2));

        List<ExpenseCategoryBreakdownDTO> result = service.getCategoryBreakdown(from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory()).isEqualTo(ExpenseCategory.RENT);
        assertThat(result.get(0).getPercentage()).isEqualTo(75.0);
        assertThat(result.get(1).getPercentage()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("getCategoryBreakdown(date range): 0% when grand total is zero")
    void getCategoryBreakdown_dateRange_zeroTotal() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        Object[] row = {"RENT", BigDecimal.ZERO};
        when(expenseRepository.sumGroupedByCategoryDateRange("test-tenant", from, to))
                .thenReturn(Collections.singletonList(row));

        List<ExpenseCategoryBreakdownDTO> result = service.getCategoryBreakdown(from, to);

        assertThat(result.get(0).getPercentage()).isEqualTo(0.0);
    }

    // ── getSummary ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary: variable = total - fixed")
    void getSummary_computesVariable() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        when(expenseRepository.sumByDateRange("test-tenant", from, to)).thenReturn(new BigDecimal("1000000"));
        when(expenseRepository.sumByDateRangeAndCategories(eq("test-tenant"), eq(from), eq(to), anyList()))
                .thenReturn(new BigDecimal("600000"));

        java.util.Map<String, Object> summary = service.getSummary(from, to);

        assertThat((BigDecimal) summary.get("total")).isEqualByComparingTo("1000000");
        assertThat((BigDecimal) summary.get("fixed")).isEqualByComparingTo("600000");
        assertThat((BigDecimal) summary.get("variable")).isEqualByComparingTo("400000");
        assertThat((BigDecimal) summary.get("netVsRevenue")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getSummary: null total and fixed default to zero")
    void getSummary_nullDefaults() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        when(expenseRepository.sumByDateRange("test-tenant", from, to)).thenReturn(null);
        when(expenseRepository.sumByDateRangeAndCategories(eq("test-tenant"), eq(from), eq(to), anyList()))
                .thenReturn(null);

        java.util.Map<String, Object> summary = service.getSummary(from, to);

        assertThat((BigDecimal) summary.get("total")).isEqualByComparingTo("0");
        assertThat((BigDecimal) summary.get("fixed")).isEqualByComparingTo("0");
        assertThat((BigDecimal) summary.get("variable")).isEqualByComparingTo("0");
    }

    // ── getChart ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getChart: default (day) granularity maps daily rows")
    void getChart_default() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        Object[] row = {"2024-01-01", new BigDecimal("100000")};
        when(expenseRepository.getDailyChart("test-tenant", from, to))
                .thenReturn(Collections.singletonList(row));

        java.util.List<java.util.Map<String, Object>> chart = service.getChart(from, to);

        assertThat(chart).hasSize(1);
        assertThat(chart.get(0).get("label")).isEqualTo("2024-01-01");
        assertThat(chart.get(0).get("value")).isEqualTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("getChart: week granularity uses weekly query")
    void getChart_week() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);
        when(expenseRepository.getWeeklyChart("test-tenant", from, to))
                .thenReturn(Collections.singletonList(new Object[]{"W1", new BigDecimal("5")}));

        assertThat(service.getChart(from, to, "week")).hasSize(1);
        verify(expenseRepository).getWeeklyChart("test-tenant", from, to);
    }

    @Test
    @DisplayName("getChart: month granularity uses monthly query")
    void getChart_month() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(expenseRepository.getMonthlyChart("test-tenant", from, to))
                .thenReturn(Collections.singletonList(new Object[]{"2024-01", new BigDecimal("5")}));

        assertThat(service.getChart(from, to, "month")).hasSize(1);
        verify(expenseRepository).getMonthlyChart("test-tenant", from, to);
    }

    @Test
    @DisplayName("getChart: year granularity uses yearly query")
    void getChart_year() {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(expenseRepository.getYearlyChart("test-tenant", from, to))
                .thenReturn(Collections.singletonList(new Object[]{"2024", new BigDecimal("5")}));

        assertThat(service.getChart(from, to, "year")).hasSize(1);
        verify(expenseRepository).getYearlyChart("test-tenant", from, to);
    }

    @Test
    @DisplayName("getChart: null granularity falls back to daily query")
    void getChart_nullGranularity() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        when(expenseRepository.getDailyChart("test-tenant", from, to)).thenReturn(List.of());

        assertThat(service.getChart(from, to, null)).isEmpty();
        verify(expenseRepository).getDailyChart("test-tenant", from, to);
    }
}

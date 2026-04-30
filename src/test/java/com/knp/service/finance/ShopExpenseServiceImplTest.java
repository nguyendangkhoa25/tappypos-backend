package com.knp.service.finance;

import com.knp.config.AuthContext;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.knp.model.dto.finance.ShopExpenseDTO;
import com.knp.model.dto.finance.ShopExpenseRequest;
import com.knp.model.entity.finance.ShopExpense;
import com.knp.model.enums.ExpenseCategory;
import com.knp.repository.finance.ShopExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopExpenseServiceImpl Unit Tests")
class ShopExpenseServiceImplTest {

    @Mock private ShopExpenseRepository expenseRepository;
    @Mock private AuthContext authContext;

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
        when(expenseRepository.sumGroupedByCategory(2024, 1)).thenReturn(rows);

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
        when(expenseRepository.sumGroupedByCategory(2024, 1)).thenReturn(Collections.singletonList(row));

        List<ExpenseCategoryBreakdownDTO> result = service.getCategoryBreakdown(2024, 1);

        assertThat(result.get(0).getPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCategoryBreakdown: returns empty list when no data")
    void getCategoryBreakdown_empty() {
        when(expenseRepository.sumGroupedByCategory(2024, 1)).thenReturn(List.of());

        assertThat(service.getCategoryBreakdown(2024, 1)).isEmpty();
    }
}

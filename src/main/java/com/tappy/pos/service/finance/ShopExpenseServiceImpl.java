package com.tappy.pos.service.finance;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseRequest;
import com.tappy.pos.model.entity.finance.ShopExpense;
import com.tappy.pos.model.enums.ExpenseCategory;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.multitenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.List.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopExpenseServiceImpl implements ShopExpenseService {

    private final ShopExpenseRepository expenseRepository;
    private final AuthContext authContext;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public ShopExpenseDTO create(ShopExpenseRequest request) {
        ShopExpense expense = ShopExpense.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .createdBy(authContext.getCurrentUsername())
                .build();
        ShopExpenseDTO saved = toDTO(expenseRepository.save(expense));
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.EXPENSE_CREATED, "EXPENSE", String.valueOf(saved.getId()),
                "Ghi chi phí: " + request.getDescription() + " — " + request.getAmount() + "đ", null);
        return saved;
    }

    @Override
    @Transactional
    public ShopExpenseDTO update(Long id, ShopExpenseRequest request) {
        ShopExpense expense = findActive(id);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setUpdatedBy(authContext.getCurrentUsername());
        return toDTO(expenseRepository.save(expense));
    }

    @Override
    @Transactional(readOnly = true)
    public ShopExpenseDTO getById(Long id) {
        return toDTO(findActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopExpenseDTO> search(LocalDate from, LocalDate to, ExpenseCategory category, Pageable pageable) {
        return expenseRepository.search(from, to, category != null ? category.name() : null, pageable).map(this::toDTO);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ShopExpense expense = findActive(id);
        expense.softDelete();
        expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryBreakdownDTO> getCategoryBreakdown(Integer year, Integer month) {
        List<Object[]> rows = expenseRepository.sumGroupedByCategory(year, month);

        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    ExpenseCategory cat = (ExpenseCategory) row[0];
                    BigDecimal total = (BigDecimal) row[1];
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                            ? total.divide(grandTotal, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return ExpenseCategoryBreakdownDTO.builder()
                            .category(cat)
                            .categoryDisplayName(cat.getDisplayName())
                            .total(total)
                            .percentage(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static final List<String> FIXED_CATEGORIES = of(
            "RENT", "ELECTRICITY", "WATER", "INTERNET", "PHONE", "SOFTWARE", "INSURANCE");

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getSummary(LocalDate from, LocalDate to) {
        BigDecimal total = expenseRepository.sumByDateRange(from, to);
        if (total == null) total = BigDecimal.ZERO;
        BigDecimal fixed = expenseRepository.sumByDateRangeAndCategories(from, to, FIXED_CATEGORIES);
        if (fixed == null) fixed = BigDecimal.ZERO;
        BigDecimal variable = total.subtract(fixed);
        return java.util.Map.of("total", total, "fixed", fixed, "variable", variable, "netVsRevenue", BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getChart(LocalDate from, LocalDate to) {
        return getChart(from, to, "day");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getChart(LocalDate from, LocalDate to, String granularity) {
        List<Object[]> rows = switch (granularity == null ? "day" : granularity) {
            case "week"  -> expenseRepository.getWeeklyChart(from, to);
            case "month" -> expenseRepository.getMonthlyChart(from, to);
            case "year"  -> expenseRepository.getYearlyChart(from, to);
            default      -> expenseRepository.getDailyChart(from, to);
        };
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : rows) { result.add(java.util.Map.of("label", row[0].toString(), "value", row[1])); }
        return result;
    }

    private ShopExpense findActive(Long id) {
        return expenseRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    private ShopExpenseDTO toDTO(ShopExpense e) {
        return ShopExpenseDTO.builder()
                .id(e.getId())
                .amount(e.getAmount())
                .category(e.getCategory())
                .categoryDisplayName(e.getCategory().getDisplayName())
                .description(e.getDescription())
                .expenseDate(e.getExpenseDate())
                .paymentMethod(e.getPaymentMethod())
                .referenceNumber(e.getReferenceNumber())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

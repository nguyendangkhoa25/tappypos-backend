package com.knp.service.finance;

import com.knp.config.AuthContext;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.knp.model.dto.finance.ShopExpenseDTO;
import com.knp.model.dto.finance.ShopExpenseRequest;
import com.knp.model.entity.finance.ShopExpense;
import com.knp.model.enums.ExpenseCategory;
import com.knp.repository.finance.ShopExpenseRepository;
import com.knp.multitenant.TenantContext;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopExpenseServiceImpl implements ShopExpenseService {

    private final ShopExpenseRepository expenseRepository;
    private final AuthContext authContext;
    private final TenantContext tenantContext;

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
        return toDTO(expenseRepository.save(expense));
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

package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseRequest;
import com.tappy.pos.model.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ShopExpenseService {

    ShopExpenseDTO create(ShopExpenseRequest request);

    ShopExpenseDTO update(Long id, ShopExpenseRequest request);

    ShopExpenseDTO getById(Long id);

    Page<ShopExpenseDTO> search(LocalDate from, LocalDate to, ExpenseCategory category, Pageable pageable);

    void delete(Long id);

    List<ExpenseCategoryBreakdownDTO> getCategoryBreakdown(Integer year, Integer month);

    java.util.Map<String, Object> getSummary(LocalDate from, LocalDate to);

    java.util.List<java.util.Map<String, Object>> getChart(LocalDate from, LocalDate to);
    java.util.List<java.util.Map<String, Object>> getChart(LocalDate from, LocalDate to, String granularity);
}

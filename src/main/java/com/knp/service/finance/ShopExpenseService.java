package com.knp.service.finance;

import com.knp.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.knp.model.dto.finance.ShopExpenseDTO;
import com.knp.model.dto.finance.ShopExpenseRequest;
import com.knp.model.enums.ExpenseCategory;
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
}

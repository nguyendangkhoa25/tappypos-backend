package com.knp.controller.finance;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.knp.model.dto.finance.ShopExpenseDTO;
import com.knp.model.dto.finance.ShopExpenseRequest;
import com.knp.model.enums.ExpenseCategory;
import com.knp.service.finance.ShopExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@RequiresFeature("EXPENSE")
public class ShopExpenseController {

    private final ShopExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShopExpenseDTO>> create(@Valid @RequestBody ShopExpenseRequest request) {
        log.info("Endpoint: POST /expenses");
        return ResponseEntity.ok(ApiResponse.success(expenseService.create(request), "Expense created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopExpenseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ShopExpenseRequest request) {
        log.info("Endpoint: PUT /expenses/{}", id);
        return ResponseEntity.ok(ApiResponse.success(expenseService.update(id, request), "Expense updated"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopExpenseDTO>> getById(@PathVariable Long id) {
        log.info("Endpoint: GET /expenses/{}", id);
        return ResponseEntity.ok(ApiResponse.success(expenseService.getById(id), "Expense retrieved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShopExpenseDTO>>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /expenses - from:{} to:{} category:{}", from, to, category);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate", "id"));
        return ResponseEntity.ok(ApiResponse.success(expenseService.search(from, to, category, pageable), "Expenses retrieved"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /expenses/{}", id);
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted"));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryBreakdownDTO>>> getCategoryBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("Endpoint: GET /expenses/category-breakdown - year:{} month:{}", year, month);
        return ResponseEntity.ok(ApiResponse.success(expenseService.getCategoryBreakdown(year, month), "Category breakdown retrieved"));
    }
}

package com.tappy.pos.controller.finance;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.finance.DefaultExpenseDTO;
import com.tappy.pos.model.dto.finance.DefaultExpenseRequest;
import com.tappy.pos.model.dto.finance.ExpenseCategoryBreakdownDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseRequest;
import com.tappy.pos.model.enums.ExpenseCategory;
import com.tappy.pos.service.finance.DefaultExpenseService;
import com.tappy.pos.service.finance.ShopExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@RequiresFeature("EXPENSE")
public class ShopExpenseController {

    private final ShopExpenseService expenseService;
    private final DefaultExpenseService defaultExpenseService;

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
        PageRequest pageable = PageRequest.of(page, size);
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

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /expenses/summary from={} to={}", from, to);
        return ResponseEntity.ok(ApiResponse.success(expenseService.getSummary(from, to), "OK"));
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        log.info("Endpoint: GET /expenses/chart from={} to={} granularity={}", from, to, granularity);
        return ResponseEntity.ok(ApiResponse.success(expenseService.getChart(from, to, granularity), "OK"));
    }

    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<List<DefaultExpenseDTO>>> getDefaults() {
        log.info("Endpoint: GET /expenses/defaults");
        return ResponseEntity.ok(ApiResponse.success(defaultExpenseService.findAll(), "OK"));
    }

    @PostMapping("/defaults")
    public ResponseEntity<ApiResponse<DefaultExpenseDTO>> createDefault(@Valid @RequestBody DefaultExpenseRequest request) {
        log.info("Endpoint: POST /expenses/defaults");
        return ResponseEntity.ok(ApiResponse.success(defaultExpenseService.create(request), "Created"));
    }

    @PutMapping("/defaults/{id}")
    public ResponseEntity<ApiResponse<DefaultExpenseDTO>> updateDefault(
            @PathVariable Long id,
            @Valid @RequestBody DefaultExpenseRequest request) {
        log.info("Endpoint: PUT /expenses/defaults/{}", id);
        return ResponseEntity.ok(ApiResponse.success(defaultExpenseService.update(id, request), "Updated"));
    }

    @DeleteMapping("/defaults/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDefault(@PathVariable Long id) {
        log.info("Endpoint: DELETE /expenses/defaults/{}", id);
        defaultExpenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    @PostMapping("/clone-defaults")
    public ResponseEntity<ApiResponse<List<ShopExpenseDTO>>> cloneDefaults(@RequestBody Map<String, String> body) {
        String month = body.get("month");
        log.info("Endpoint: POST /expenses/clone-defaults month={}", month);
        return ResponseEntity.ok(ApiResponse.success(defaultExpenseService.cloneToMonth(month), "Cloned"));
    }
}

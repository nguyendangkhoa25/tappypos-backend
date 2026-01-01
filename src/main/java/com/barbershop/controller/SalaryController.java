package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.salary.*;
import com.barbershop.model.entity.Salary;
import com.barbershop.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    /**
     * POST /api/salaries
     * Create a new salary record
     * Request Body:
     * - employeeId: ID of the employee (required)
     * - month: Month (1-12, required)
     * - year: Year (required)
     * - deductions: Deduction amount (optional, default: 0)
     * - overtime: Overtime amount (optional, default: 0)
     * - bonus: Bonus amount (optional, default: 0)
     * - notes: Additional notes (optional)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SalaryDTO>> createSalary(@RequestBody CreateSalaryRequest request) {
        log.info("Endpoint: POST /salaries - Create salary for employee {} for {}/{}",
                request.getEmployeeId(), request.getMonth(), request.getYear());

        SalaryDTO salary = salaryService.createSalary(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(salary, "Salary created successfully"));
    }

    /**
     * GET /api/salaries/{id}
     * Get salary by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalaryDTO>> getSalaryById(@PathVariable Long id) {
        log.info("Endpoint: GET /salaries/{} - Get salary by ID", id);

        SalaryDTO salary = salaryService.getSalaryById(id);
        return ResponseEntity.ok(ApiResponse.success(salary, "Salary retrieved successfully"));
    }

    /**
     * GET /api/salaries/detail/{id}
     * Get detailed salary information including order items
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<SalaryDetailDTO>> getSalaryDetail(@PathVariable Long id) {
        log.info("Endpoint: GET /salaries/detail/{} - Get detailed salary", id);

        SalaryDetailDTO salary = salaryService.getSalaryDetail(id);
        return ResponseEntity.ok(ApiResponse.success(salary, "Salary detail retrieved successfully"));
    }

    /**
     * GET /api/salaries/employee/{employeeId}/month/{month}/year/{year}
     * Get salary by employee, month, and year
     */
    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}")
    public ResponseEntity<ApiResponse<SalaryDTO>> getSalaryByEmployeeAndMonthYear(
            @PathVariable Long employeeId,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        log.info("Endpoint: GET /salaries/employee/{}/month/{}/year/{} - Get salary by employee/month/year",
                employeeId, month, year);

        SalaryDTO salary = salaryService.getSalaryByEmployeeAndMonthYear(employeeId, month, year);
        if (salary == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "Salary not found for this employee in this month"));
        }
        return ResponseEntity.ok(ApiResponse.success(salary, "Salary retrieved successfully"));
    }

    /**
     * GET /api/salaries/employee/{employeeId}
     * Get all salaries for an employee with pagination
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<Page<SalaryDTO>>> getSalariesByEmployee(
            @PathVariable Long employeeId,
            Pageable pageable) {
        log.info("Endpoint: GET /salaries/employee/{} - Get salaries for employee", employeeId);

        Page<SalaryDTO> salaries = salaryService.getSalariesByEmployee(employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(salaries, "Salaries retrieved successfully"));
    }

    /**
     * GET /api/salaries
     * Get all salaries with pagination
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - status: Filter by status (DRAFT, SUBMITTED, APPROVED, REJECTED) (optional)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SalaryDTO>>> getAllSalaries(
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        log.info("Endpoint: GET /salaries - Get all salaries with status filter: {}", status);

        Page<SalaryDTO> salaries;
        if (status != null && !status.trim().isEmpty()) {
            try {
                Salary.SalaryStatus statusEnum = Salary.SalaryStatus.valueOf(status.toUpperCase());
                salaries = salaryService.getSalariesByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status provided: {}", status);
                salaries = salaryService.getAllSalaries(pageable);
            }
        } else {
            salaries = salaryService.getAllSalaries(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(salaries, "Salaries retrieved successfully"));
    }

    /**
     * PUT /api/salaries/{id}
     * Update salary adjustments
     * Request Body:
     * - deductions: Deduction amount (optional)
     * - overtime: Overtime amount (optional)
     * - bonus: Bonus amount (optional)
     * - notes: Additional notes (optional)
     * - status: Salary status (optional)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalaryDTO>> updateSalary(
            @PathVariable Long id,
            @RequestBody UpdateSalaryRequest request) {
        log.info("Endpoint: PUT /salaries/{} - Update salary", id);

        SalaryDTO salary = salaryService.updateSalary(id, request);
        return ResponseEntity.ok(ApiResponse.success(salary, "Salary updated successfully"));
    }

    /**
     * DELETE /api/salaries/{id}
     * Delete (soft delete) a salary
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSalary(@PathVariable Long id) {
        log.info("Endpoint: DELETE /salaries/{} - Delete salary", id);

        salaryService.deleteSalary(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Salary deleted successfully"));
    }

    /**
     * GET /api/salaries/uncalculated-items/{employeeId}/month/{month}/year/{year}
     * Get all uncalculated order items for an employee in a given month/year
     * These are completed items that haven't been included in any salary yet
     */
    @GetMapping("/uncalculated-items/{employeeId}/month/{month}/year/{year}")
    public ResponseEntity<ApiResponse<List<OrderItemEarningDTO>>> getUncalculatedOrderItems(
            @PathVariable Long employeeId,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        log.info("Endpoint: GET /salaries/uncalculated-items/{}/month/{}/year/{} - Get uncalculated order items",
                employeeId, month, year);

        List<OrderItemEarningDTO> items = salaryService.getUncalculatedOrderItems(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.success(items, "Uncalculated order items retrieved successfully"));
    }

    /**
     * GET /api/salaries/{salaryId}/order-items
     * Get all order items included in a specific salary
     */
    @GetMapping("/{salaryId}/order-items")
    public ResponseEntity<ApiResponse<List<OrderItemEarningDTO>>> getOrderItemsBySalaryId(
            @PathVariable Long salaryId) {
        log.info("Endpoint: GET /salaries/{}/order-items - Get order items by salary ID", salaryId);

        List<OrderItemEarningDTO> items = salaryService.getOrderItemsBySalaryId(salaryId);
        return ResponseEntity.ok(ApiResponse.success(items, "Order items for salary retrieved successfully"));
    }

    /**
     * GET /api/salaries/items/{employeeId}
     * Get order items for an employee with optional filter by calculated status
     * Returns items that are completed and optionally filtered by calculation status
     * Sorted by completed date in descending order (newest first)
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - calculated: Filter by calculated status (true for calculated, false/missing for uncalculated, default: false)
     */
    @GetMapping("/items/{employeeId}")
    public ResponseEntity<ApiResponse<Page<OrderItemEarningDTO>>> getOrderItems(
            @PathVariable Long employeeId,
            @RequestParam(value = "calculated", required = false, defaultValue = "false") Boolean calculated,
            Pageable pageable) {
        log.info("Endpoint: GET /salaries/items/{} - Get order items for employee with calculated={}",
                employeeId, calculated);
        Page<OrderItemEarningDTO> items = salaryService.getOrderItemsByEmployeeAndCalculatedStatus(
                employeeId, calculated, pageable);

        return ResponseEntity.ok(ApiResponse.success(items, "Order items retrieved successfully"));
    }
}


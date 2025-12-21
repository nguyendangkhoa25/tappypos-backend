package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.employee.CreateEmployeeRequest;
import com.barbershop.model.dto.employee.EmployeeDTO;
import com.barbershop.model.dto.employee.EmployeeEarningsDTO;
import com.barbershop.model.dto.employee.UpdateEmployeeRequest;
import com.barbershop.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * POST /api/employees
     * Create a new employee
     *
     * Request Body:
     * - name: Employee name (required)
     * - phone: Phone number (required, unique)
     * - email: Email address (optional, unique)
     * - position: Job position (required)
     * - hireDate: Date of hire (optional, defaults to today)
     * - description: Employee description (optional)
     * - baseSalary: Base salary (optional, defaults to 0)
     *
     * Examples:
     * - POST /api/employees
     *   {
     *     "name": "John Doe",
     *     "phone": "0123456789",
     *     "email": "john@example.com",
     *     "position": "Barber",
     *     "baseSalary": 5000000
     *   }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDTO>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        log.info("Endpoint: POST /employees - Create new employee - name: {}, phone: {}, email: {}",
                request.getName(), request.getPhone(), request.getEmail());
        EmployeeDTO employee = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(employee, "Employee created successfully"));
    }

    /**
     * GET /api/employees
     * Get all employees with optional pagination, status filtering, search, and sorting
     *
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - search: Search term to search in name, phone, email, and position (optional)
     * - status: Filter by status - "ACTIVE", "INACTIVE", or "ON_LEAVE" (optional)
     * - sortBy: Sort by field name (e.g., "id", "name", "baseSalary", "hireDate") (optional, default: "id")
     * - sortDirection: Sort direction "ASC" or "DESC" (optional, default: "DESC")
     *
     * Examples:
     * - GET /api/employees?page=0&size=20
     * - GET /api/employees?page=0&size=20&search=john
     * - GET /api/employees?page=0&size=20&status=ACTIVE
     * - GET /api/employees?page=0&size=20&search=john&status=ACTIVE&sortBy=baseSalary&sortDirection=DESC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeDTO>>> getAllEmployees(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            Pageable pageable) {
        log.info("Endpoint: GET /employees - Get all employees with filters - search: {}, status: {}, page: {}, size: {}",
                searchTerm, status, pageable.getPageNumber(), pageable.getPageSize());
        Page<EmployeeDTO> employees = employeeService.getAllEmployees(searchTerm, status, sortBy, sortDirection, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully"));
    }

    /**
     * GET /api/employees/{id}
     * Get employee details by ID
     *
     * Path Parameters:
     * - id: Employee ID (required)
     *
     * Examples:
     * - GET /api/employees/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        log.info("Endpoint: GET /employees/{} - Get employee by ID", id);
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee retrieved successfully"));
    }

    /**
     * PUT /api/employees/{id}
     * Update an existing employee
     *
     * Path Parameters:
     * - id: Employee ID (required)
     *
     * Request Body:
     * - name: Employee name (optional)
     * - email: Email address (optional, unique)
     * - position: Job position (optional)
     * - status: Employee status - "ACTIVE", "INACTIVE", or "ON_LEAVE" (optional)
     * - description: Employee description (optional)
     * - baseSalary: Base salary (optional)
     *
     * Examples:
     * - PUT /api/employees/1
     *   {
     *     "name": "Jane Doe",
     *     "position": "Senior Barber",
     *     "baseSalary": 6000000
     *   }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployee(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeRequest request) {
        log.info("Endpoint: PUT /employees/{} - Update employee", id);
        EmployeeDTO employee = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee updated successfully"));
    }

    /**
     * DELETE /api/employees/{id}
     * Delete (soft delete) an employee
     *
     * Path Parameters:
     * - id: Employee ID (required)
     *
     * Examples:
     * - DELETE /api/employees/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        log.info("Endpoint: DELETE /employees/{} - Delete employee", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee deleted successfully"));
    }

    /**
     * GET /api/employees/{id}/earnings
     * Get employee earnings summary
     *
     * Path Parameters:
     * - id: Employee ID (required)
     *
     * Returns:
     * - employeeId: Employee ID
     * - employeeName: Employee name
     * - totalEarned: Total earnings from completed orders
     * - completedOrderCount: Number of completed orders
     *
     * Examples:
     * - GET /api/employees/1/earnings
     */
    @GetMapping("/{id}/earnings")
    public ResponseEntity<ApiResponse<EmployeeEarningsDTO>> getEmployeeEarnings(@PathVariable Long id) {
        log.info("Endpoint: GET /employees/{}/earnings - Get employee earnings", id);
        EmployeeEarningsDTO earnings = employeeService.getEmployeeEarnings(id);
        return ResponseEntity.ok(ApiResponse.success(earnings, "Employee earnings retrieved successfully"));
    }
}


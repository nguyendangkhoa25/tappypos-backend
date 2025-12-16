package com.barbershop.controller;

import com.barbershop.model.dto.*;
import com.barbershop.model.dto.employee.CreateEmployeeRequest;
import com.barbershop.model.dto.employee.EmployeeDTO;
import com.barbershop.model.dto.employee.EmployeeEarningsDTO;
import com.barbershop.model.dto.employee.UpdateEmployeeRequest;
import com.barbershop.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDTO>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        EmployeeDTO employee = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(employee, "Employee created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeDTO>>> getAllEmployees(Pageable pageable) {
        Page<EmployeeDTO> employees = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployee(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeRequest request) {
        EmployeeDTO employee = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee deleted successfully"));
    }

    @GetMapping("/{id}/earnings")
    public ResponseEntity<ApiResponse<EmployeeEarningsDTO>> getEmployeeEarnings(@PathVariable Long id) {
        EmployeeEarningsDTO earnings = employeeService.getEmployeeEarnings(id);
        return ResponseEntity.ok(ApiResponse.success(earnings, "Employee earnings retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<EmployeeDTO>>> searchEmployees(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<EmployeeDTO> employees = employeeService.searchEmployees(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees, "Search results retrieved successfully"));
    }
}


package com.tappy.pos.controller.employee;

import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.service.employee.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@RequiresFeature("EMPLOYEE")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /employees search={} page={} size={}", search, page, size);
        return ResponseEntity.ok(employeeService.getAll(search, page, size));
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeDTO>> getAllActive() {
        log.info("Endpoint: GET /employees/all");
        return ResponseEntity.ok(employeeService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getById(@PathVariable Long id) {
        log.info("Endpoint: GET /employees/{}", id);
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<EmployeeDTO> getByUserId(@PathVariable Long userId) {
        log.info("Endpoint: GET /employees/by-user/{}", userId);
        return ResponseEntity.ok(employeeService.getByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Endpoint: POST /employees");
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        log.info("Endpoint: PUT /employees/{}", id);
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /employees/{}", id);
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.tappy.pos.controller.employee;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.service.employee.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * GET /employees/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=day|week|month&limit=10
     *
     * Returns:
     *  - summary        : { totalRevenue, totalCommission, activeEmployeeCount, avgRevenuePerEmployee }
     *  - rankingRevenue : [ { employeeName, userId, orderCount, revenue } ]
     *  - rankingCommission : [ { employeeId, employeeName, commission, orderCount, revenue } ]
     *  - trend          : [ { label, revenue, commission } ]
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analytics(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Endpoint: GET /employees/analytics from={} to={} granularity={}", from, to, granularity);

        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).atTime(23, 59, 59);

        return ResponseEntity.ok(ApiResponse.success(
                employeeService.getAnalytics(fromDt, toDt, granularity, limit), "OK"));
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    /**
     * POST /employees/{id}/avatar  (multipart/form-data, field: "file")
     * Upload / replace employee avatar. Resized to 256×256 JPEG on the server.
     */
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmployeeDTO>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("Endpoint: POST /employees/{}/avatar", id);
        return ResponseEntity.ok(ApiResponse.success(employeeService.uploadAvatar(id, file), "Ảnh đại diện đã được cập nhật"));
    }

    /**
     * DELETE /employees/{id}/avatar
     * Remove employee avatar from R2 and clear the URL.
     */
    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<EmployeeDTO>> deleteAvatar(@PathVariable Long id) {
        log.info("Endpoint: DELETE /employees/{}/avatar", id);
        return ResponseEntity.ok(ApiResponse.success(employeeService.deleteAvatar(id), "Đã xoá ảnh đại diện"));
    }
}

package com.tappy.pos.controller.employee;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.repository.order.OrderRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@RequiresFeature("EMPLOYEE")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

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

        // ── Summary ──────────────────────────────────────────────────────────
        BigDecimal totalRevenue   = orderRepository.sumRevenueByDateRange(fromDt, toDt);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal totalCommission = orderItemRepository.sumTeamCommissionByDateRange(fromDt, toDt);
        if (totalCommission == null) totalCommission = BigDecimal.ZERO;

        long activeEmployeeCount = orderRepository.countActiveEmployees(fromDt, toDt);
        double avgRevenuePerEmployee = activeEmployeeCount > 0
                ? totalRevenue.doubleValue() / activeEmployeeCount : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRevenue",          totalRevenue.doubleValue());
        summary.put("totalCommission",        totalCommission.doubleValue());
        summary.put("activeEmployeeCount",    activeEmployeeCount);
        summary.put("avgRevenuePerEmployee",  avgRevenuePerEmployee);

        // ── Revenue ranking ───────────────────────────────────────────────────
        List<Object[]> revRows = orderRepository.getEmployeeRevenueRankingByDateRange(fromDt, toDt, Math.max(1, limit));
        List<Map<String, Object>> rankingRevenue = revRows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("employeeName", r[0] != null ? r[0].toString() : "");
            item.put("userId",       r[1] != null ? r[1].toString() : null);
            item.put("orderCount",   r[2] != null ? ((Number) r[2]).longValue()   : 0);
            item.put("revenue",      r[3] != null ? ((Number) r[3]).doubleValue() : 0);
            return item;
        }).collect(Collectors.toList());

        // ── Commission ranking ────────────────────────────────────────────────
        List<Object[]> commRows = orderItemRepository.getEmployeeCommissionRankingByDateRange(fromDt, toDt, Math.max(1, limit));
        List<Map<String, Object>> rankingCommission = commRows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("employeeId",   r[0] != null ? r[0].toString() : null);
            item.put("employeeName", r[1] != null ? r[1].toString() : "");
            item.put("commission",   r[2] != null ? ((Number) r[2]).doubleValue() : 0);
            item.put("orderCount",   r[3] != null ? ((Number) r[3]).longValue()   : 0);
            item.put("revenue",      r[4] != null ? ((Number) r[4]).doubleValue() : 0);
            return item;
        }).collect(Collectors.toList());

        // ── Trend (revenue + commission merged by label) ──────────────────────
        List<Object[]> revTrend = switch (granularity) {
            case "week"  -> orderRepository.getEmployeeRevenueTrendByWeek(fromDt, toDt);
            case "month" -> orderRepository.getEmployeeRevenueTrendByMonth(fromDt, toDt);
            default      -> orderRepository.getEmployeeRevenueTrendByDay(fromDt, toDt);
        };
        List<Object[]> commTrend = switch (granularity) {
            case "week"  -> orderItemRepository.getTeamCommissionTrendByWeek(fromDt, toDt);
            case "month" -> orderItemRepository.getTeamCommissionTrendByMonth(fromDt, toDt);
            default      -> orderItemRepository.getTeamCommissionTrendByDay(fromDt, toDt);
        };

        // Merge by label (outer-join style)
        Map<String, double[]> trendMap = new LinkedHashMap<>();
        for (Object[] r : revTrend) {
            String lbl = r[0] != null ? r[0].toString() : "";
            trendMap.computeIfAbsent(lbl, k -> new double[]{0, 0})[0] =
                    r[1] != null ? ((Number) r[1]).doubleValue() : 0;
        }
        for (Object[] r : commTrend) {
            String lbl = r[0] != null ? r[0].toString() : "";
            trendMap.computeIfAbsent(lbl, k -> new double[]{0, 0})[1] =
                    r[1] != null ? ((Number) r[1]).doubleValue() : 0;
        }
        List<Map<String, Object>> trend = trendMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("label",      e.getKey());
                    point.put("revenue",    e.getValue()[0]);
                    point.put("commission", e.getValue()[1]);
                    return point;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",            summary);
        result.put("rankingRevenue",      rankingRevenue);
        result.put("rankingCommission",   rankingCommission);
        result.put("trend",               trend);

        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
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

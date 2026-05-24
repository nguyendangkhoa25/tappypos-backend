package com.tappy.pos.controller.appointment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.appointment.*;
import com.tappy.pos.repository.appointment.AppointmentRepository;
import com.tappy.pos.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@RequiresFeature("APPOINTMENT")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("GET /appointments?date={}", date);
        Page<AppointmentDTO> result = appointmentService.getByDate(date, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Appointments retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> getById(@PathVariable Long id) {
        log.info("GET /appointments/{}", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getById(id), "Appointment retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentDTO>> create(
            @Valid @RequestBody CreateAppointmentRequest request) {
        log.info("POST /appointments - customer: {}", request.getCustomerName());
        AppointmentDTO created = appointmentService.create(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Lịch hẹn đã được tạo"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> update(
            @PathVariable Long id,
            @RequestBody UpdateAppointmentRequest request) {
        log.info("PUT /appointments/{}", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.update(id, request), "Lịch hẹn đã được cập nhật"));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<AppointmentDTO>> confirm(@PathVariable Long id) {
        log.info("PUT /appointments/{}/confirm", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.confirm(id), "Đã xác nhận lịch hẹn"));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<CheckInPayload>> checkIn(@PathVariable Long id) {
        log.info("POST /appointments/{}/check-in", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.checkIn(id), "Khách đã check-in"));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentDTO>> cancel(@PathVariable Long id) {
        log.info("PUT /appointments/{}/cancel", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.cancel(id), "Đã huỷ lịch hẹn"));
    }

    @PutMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<AppointmentDTO>> noShow(@PathVariable Long id) {
        log.info("PUT /appointments/{}/no-show", id);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.noShow(id), "Đã đánh dấu không đến"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /appointments/{}", id);
        appointmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá lịch hẹn"));
    }

    /**
     * GET /appointments/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=day|week|month&limit=10
     *
     * Returns:
     *  - summary          : { total, completedCount, completionRate, cancelledCount, avgPerDay }
     *  - trend            : [ { label, total, completed, cancelled } ]
     *  - rankingServices  : [ { name, count } ]
     *  - rankingEmployees : [ { name, count } ]
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analytics(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /appointments/analytics from={} to={} granularity={}", from, to, granularity);

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate   = LocalDate.parse(to);

        // ── Summary ──────────────────────────────────────────────────────────
        Object[] sumRow       = appointmentRepository.getAnalyticsSummary(fromDate, toDate);
        long total            = sumRow[0] != null ? ((Number) sumRow[0]).longValue() : 0;
        long completedCount   = sumRow[1] != null ? ((Number) sumRow[1]).longValue() : 0;
        long cancelledCount   = sumRow[2] != null ? ((Number) sumRow[2]).longValue() : 0;
        long days             = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        double completionRate = total > 0 ? Math.round(completedCount * 1000.0 / total) / 1000.0 : 0.0;
        double avgPerDay      = days > 0 ? Math.round(total * 10.0 / days) / 10.0 : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total",           total);
        summary.put("completedCount",  completedCount);
        summary.put("completionRate",  completionRate);
        summary.put("cancelledCount",  cancelledCount);
        summary.put("avgPerDay",       avgPerDay);

        // ── Trend ─────────────────────────────────────────────────────────────
        List<Object[]> trendRows = switch (granularity) {
            case "week"  -> appointmentRepository.getAnalyticsTrendByWeek(fromDate, toDate);
            case "month" -> appointmentRepository.getAnalyticsTrendByMonth(fromDate, toDate);
            default      -> appointmentRepository.getAnalyticsTrendByDay(fromDate, toDate);
        };
        List<Map<String, Object>> trend = trendRows.stream().map(r -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("label",     r[0] != null ? r[0].toString() : "");
            p.put("total",     r[1] != null ? ((Number) r[1]).longValue() : 0);
            p.put("completed", r[2] != null ? ((Number) r[2]).longValue() : 0);
            p.put("cancelled", r[3] != null ? ((Number) r[3]).longValue() : 0);
            return p;
        }).collect(Collectors.toList());

        // ── Service ranking ───────────────────────────────────────────────────
        List<Object[]> svcRows = appointmentRepository.getServiceRanking(fromDate, toDate, Math.max(1, limit));
        List<Map<String, Object>> rankingServices = svcRows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name",  r[0] != null ? r[0].toString() : "");
            item.put("count", r[1] != null ? ((Number) r[1]).longValue() : 0);
            return item;
        }).collect(Collectors.toList());

        // ── Employee ranking ──────────────────────────────────────────────────
        List<Object[]> empRows = appointmentRepository.getEmployeeRanking(fromDate, toDate, Math.max(1, limit));
        List<Map<String, Object>> rankingEmployees = empRows.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name",  r[0] != null ? r[0].toString() : "");
            item.put("count", r[1] != null ? ((Number) r[1]).longValue() : 0);
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",          summary);
        result.put("trend",            trend);
        result.put("rankingServices",  rankingServices);
        result.put("rankingEmployees", rankingEmployees);

        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
    }

    /**
     * Returns appointment counts grouped by date for the given date range.
     * Used by the mobile week-strip calendar to show appointment badges per day.
     * Days with zero appointments are omitted from the result map.
     *
     * @param from ISO date (yyyy-MM-dd), inclusive
     * @param to   ISO date (yyyy-MM-dd), inclusive
     */
    @GetMapping("/week-summary")
    public ResponseEntity<ApiResponse<AppointmentWeekSummaryDTO>> getWeekSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("GET /appointments/week-summary?from={}&to={}", from, to);
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getWeekSummary(from, to), "Week summary retrieved"));
    }
}

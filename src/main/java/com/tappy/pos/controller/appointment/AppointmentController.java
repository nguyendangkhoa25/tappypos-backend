package com.tappy.pos.controller.appointment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.appointment.*;
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
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@RequiresFeature("APPOINTMENT")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("GET /appointments?date={}", date);
        Page<AppointmentDTO> result = appointmentService.getByDate(date, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Appointments retrieved"));
    }

    @GetMapping("/by-customer")
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getByCustomer(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /appointments/by-customer?customerId={}", customerId);
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getByCustomer(customerId, PageRequest.of(page, size)),
                "Customer appointments retrieved"));
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
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getAnalytics(LocalDate.parse(from), LocalDate.parse(to), granularity, limit), "OK"));
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

package com.tappy.pos.controller.booking;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.booking.BookingDTO;
import com.tappy.pos.model.dto.booking.BookingResourceDTO;
import com.tappy.pos.model.dto.booking.BookingResourceRequest;
import com.tappy.pos.model.dto.booking.CreateBookingRequest;
import com.tappy.pos.service.booking.BookingService;
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

@Slf4j
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@RequiresFeature("BOOKING")
public class BookingController {

    private final BookingService bookingService;

    // ── Resources ────────────────────────────────────────────────────────────

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<BookingResourceDTO>>> getResources() {
        log.info("GET /bookings/resources");
        return ResponseEntity.ok(ApiResponse.success(bookingService.getResources(), "Resources retrieved"));
    }

    @PostMapping("/resources")
    public ResponseEntity<ApiResponse<BookingResourceDTO>> createResource(
            @Valid @RequestBody BookingResourceRequest request) {
        log.info("POST /bookings/resources - {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(bookingService.createResource(request), "Đã tạo bàn/sân"));
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<BookingResourceDTO>> updateResource(
            @PathVariable Long id, @RequestBody BookingResourceRequest request) {
        log.info("PUT /bookings/resources/{}", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.updateResource(id, request), "Đã cập nhật bàn/sân"));
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        log.info("DELETE /bookings/resources/{}", id);
        bookingService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá bàn/sân"));
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("GET /bookings?date={}&status={}", date, status);
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getByDate(date, status, PageRequest.of(page, size)), "Bookings retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDTO>> getById(@PathVariable Long id) {
        log.info("GET /bookings/{}", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.getById(id), "Booking retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDTO>> create(@Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /bookings - resource {} type {}", request.getResourceId(), request.getBookingType());
        return ResponseEntity.ok(ApiResponse.success(bookingService.create(request), "Đã tạo lượt đặt"));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingDTO>> checkIn(@PathVariable Long id) {
        log.info("POST /bookings/{}/check-in", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.checkIn(id), "Đã bắt đầu tính giờ"));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<BookingDTO>> checkout(@PathVariable Long id) {
        log.info("POST /bookings/{}/checkout", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.checkout(id), "Đã kết thúc và tạo hoá đơn"));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDTO>> cancel(@PathVariable Long id) {
        log.info("PUT /bookings/{}/cancel", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.cancel(id), "Đã huỷ"));
    }

    @PutMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<BookingDTO>> noShow(@PathVariable Long id) {
        log.info("PUT /bookings/{}/no-show", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.noShow(id), "Đã đánh dấu không đến"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /bookings/{}", id);
        bookingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá"));
    }
}

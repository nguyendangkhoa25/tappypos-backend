package com.tappy.pos.controller.table;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.table.CreateTableRequest;
import com.tappy.pos.model.dto.table.CreateTableReservationRequest;
import com.tappy.pos.model.dto.table.SetTableStatusRequest;
import com.tappy.pos.model.dto.table.TableDTO;
import com.tappy.pos.model.dto.table.TableReservationDTO;
import com.tappy.pos.model.dto.table.UpdateTableRequest;
import com.tappy.pos.service.table.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
@RequiresFeature("TABLE_SERVICE")
public class TableController {

    private final TableService tableService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TableDTO>>> getTables() {
        return ResponseEntity.ok(ApiResponse.success(tableService.getTables(), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableDTO>> createTable(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tableService.createTable(request), "Tạo bàn thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableDTO>> updateTable(
            @PathVariable Long id, @RequestBody UpdateTableRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tableService.updateTable(id, request), "Cập nhật bàn thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bàn thành công"));
    }

    /**
     * Staff manually sets a table status: RESERVED (+ name/time), CLEANING, or AVAILABLE.
     * OCCUPIED transitions are handled by the cart/checkout flow, not this endpoint.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TableDTO>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody SetTableStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tableService.setStatus(id, request), "Cập nhật trạng thái bàn thành công"));
    }

    // ── Advance reservation calendar (đặt bàn trước) ───────────────────────────

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<TableReservationDTO>> createReservation(
            @Valid @RequestBody CreateTableReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tableService.createReservation(request), "Đã đặt bàn"));
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<List<TableReservationDTO>>> listReservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(tableService.listReservations(from, to), "Danh sách đặt bàn"));
    }

    @PostMapping("/reservations/{id}/seat")
    public ResponseEntity<ApiResponse<TableReservationDTO>> seatReservation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tableService.seatReservation(id), "Đã nhận bàn"));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<ApiResponse<TableReservationDTO>> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tableService.cancelReservation(id), "Đã huỷ đặt bàn"));
    }

    @PostMapping("/reservations/{id}/no-show")
    public ResponseEntity<ApiResponse<TableReservationDTO>> noShowReservation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tableService.markReservationNoShow(id), "Đã đánh dấu vắng mặt"));
    }
}

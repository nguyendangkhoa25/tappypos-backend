package com.tappy.pos.controller.repair;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.repair.*;
import com.tappy.pos.service.repair.RepairTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/repair-tickets")
@RequiredArgsConstructor
@RequiresFeature("REPAIR")
public class RepairTicketController {

    private final RepairTicketService repairTicketService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RepairTicketDTO>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /repair-tickets?status={}&keyword={}", status, keyword);
        Page<RepairTicketDTO> result = repairTicketService.search(status, keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Repair tickets retrieved"));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> statusCounts() {
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.statusCounts(), "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairTicketDTO>> getById(@PathVariable Long id) {
        log.info("GET /repair-tickets/{}", id);
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.getById(id), "Repair ticket retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RepairTicketDTO>> create(
            @Valid @RequestBody CreateRepairTicketRequest request) {
        log.info("POST /repair-tickets - customer: {}", request.getCustomerName());
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.create(request), "Đã tạo phiếu sửa chữa"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairTicketDTO>> update(
            @PathVariable Long id, @RequestBody UpdateRepairTicketRequest request) {
        log.info("PUT /repair-tickets/{}", id);
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.update(id, request), "Đã cập nhật phiếu sửa chữa"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RepairTicketDTO>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateRepairStatusRequest request) {
        log.info("PUT /repair-tickets/{}/status -> {}", id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.updateStatus(id, request), "Đã cập nhật tình trạng"));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<RepairTicketDTO>> assignTechnician(
            @PathVariable Long id, @RequestBody AssignTechnicianRequest request) {
        log.info("PUT /repair-tickets/{}/assign", id);
        return ResponseEntity.ok(ApiResponse.success(repairTicketService.assignTechnician(id, request), "Đã giao thợ"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /repair-tickets/{}", id);
        repairTicketService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá phiếu sửa chữa"));
    }
}

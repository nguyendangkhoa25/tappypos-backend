package com.tappy.pos.controller.consignment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.consignment.ConsignmentDTO;
import com.tappy.pos.model.dto.consignment.ConsignmentRequest;
import com.tappy.pos.model.dto.consignment.ConsignmentSettlementDTO;
import com.tappy.pos.model.enums.ConsignmentStatus;
import com.tappy.pos.service.consignment.ConsignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/consignments")
@RequiredArgsConstructor
@RequiresFeature("CONSIGNMENT")
public class ConsignmentController {

    private final ConsignmentService consignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsignmentDTO>> create(@Valid @RequestBody ConsignmentRequest request) {
        log.info("Endpoint: POST /consignments");
        return ResponseEntity.ok(ApiResponse.success(consignmentService.create(request), "Consignment created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsignmentDTO>> update(
            @PathVariable Long id, @Valid @RequestBody ConsignmentRequest request) {
        log.info("Endpoint: PUT /consignments/{}", id);
        return ResponseEntity.ok(ApiResponse.success(consignmentService.update(id, request), "Consignment updated"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsignmentDTO>> getById(@PathVariable Long id) {
        log.info("Endpoint: GET /consignments/{}", id);
        return ResponseEntity.ok(ApiResponse.success(consignmentService.getById(id), "Consignment retrieved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConsignmentDTO>>> search(
            @RequestParam(required = false) ConsignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /consignments - status:{}", status);
        return ResponseEntity.ok(ApiResponse.success(
                consignmentService.search(status, PageRequest.of(page, size)), "Consignments retrieved"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /consignments/{}", id);
        consignmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Consignment deleted"));
    }

    @GetMapping("/{id}/settlement")
    public ResponseEntity<ApiResponse<ConsignmentSettlementDTO>> getSettlement(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /consignments/{}/settlement from={} to={}", id, from, to);
        return ResponseEntity.ok(ApiResponse.success(
                consignmentService.getSettlement(id, from, to), "Settlement computed"));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<ConsignmentDTO>> settle(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: POST /consignments/{}/settle from={} to={}", id, from, to);
        return ResponseEntity.ok(ApiResponse.success(consignmentService.settle(id, from, to), "Consignment settled"));
    }
}

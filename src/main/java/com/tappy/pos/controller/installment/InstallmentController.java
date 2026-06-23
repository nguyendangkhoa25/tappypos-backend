package com.tappy.pos.controller.installment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.installment.CreateInstallmentRequest;
import com.tappy.pos.model.dto.installment.InstallmentDTO;
import com.tappy.pos.model.dto.installment.PayInstallmentRequest;
import com.tappy.pos.service.installment.InstallmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Trả-góp (installment) contracts. Gated by the INSTALLMENT feature; the service further scopes
 * list/detail by INSTALLMENT_VIEW_ALL. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4e.
 */
@RestController
@RequestMapping("/installments")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("INSTALLMENT")
public class InstallmentController {

    private final InstallmentService installmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<InstallmentDTO>> create(@Valid @RequestBody CreateInstallmentRequest request) {
        log.info("Request: Create installment contract");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(installmentService.create(request), "Installment created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InstallmentDTO>>> search(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(installmentService.search(pageable), "OK"));
    }

    @GetMapping("/{debtId}")
    public ResponseEntity<ApiResponse<InstallmentDTO>> getById(@PathVariable Long debtId) {
        return ResponseEntity.ok(ApiResponse.success(installmentService.getById(debtId), "OK"));
    }

    @PostMapping("/schedule/{scheduleId}/pay")
    public ResponseEntity<ApiResponse<InstallmentDTO>> payPeriod(
            @PathVariable Long scheduleId, @RequestBody(required = false) PayInstallmentRequest request) {
        log.info("Request: Pay installment kỳ {}", scheduleId);
        return ResponseEntity.ok(ApiResponse.success(
                installmentService.payPeriod(scheduleId, request != null ? request : new PayInstallmentRequest()),
                "Installment payment recorded"));
    }

    @PatchMapping("/{debtId}/cancel")
    public ResponseEntity<ApiResponse<InstallmentDTO>> cancel(
            @PathVariable Long debtId, @RequestBody(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(installmentService.cancel(debtId, reason), "Installment cancelled"));
    }
}

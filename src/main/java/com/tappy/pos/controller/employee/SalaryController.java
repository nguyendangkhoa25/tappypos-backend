package com.tappy.pos.controller.employee;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.employee.*;
import com.tappy.pos.service.employee.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
@RequiresFeature("SALARY")
public class SalaryController {

    private final SalaryService salaryService;

    @PostMapping("/generate")
    public ResponseEntity<List<SalaryDTO>> generate(@RequestBody GenerateSalaryRequest request) {
        log.info("Endpoint: POST /salary/generate month={} year={}", request.getMonth(), request.getYear());
        return ResponseEntity.ok(salaryService.generatePayroll(request));
    }

    @GetMapping
    public ResponseEntity<Page<SalaryDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /salary status={} year={} month={} page={} size={}", status, year, month, page, size);
        return ResponseEntity.ok(salaryService.getSalaries(status, year, month, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryDTO> detail(@PathVariable Long id) {
        log.info("Endpoint: GET /salary/{}", id);
        return ResponseEntity.ok(salaryService.getSalaryDetail(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<SalaryDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveSalaryRequest request) {
        log.info("Endpoint: PUT /salary/{}/approve sendNotification={}", id,
                request != null && request.isSendNotification());
        return ResponseEntity.ok(salaryService.approve(id, request != null ? request : new ApproveSalaryRequest()));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<SalaryDTO> pay(
            @PathVariable Long id,
            @RequestBody(required = false) PaySalaryRequest request) {
        log.info("Endpoint: PUT /salary/{}/pay sendNotification={}", id,
                request != null && request.isSendNotification());
        return ResponseEntity.ok(salaryService.markPaid(id, request != null ? request : new PaySalaryRequest()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /salary/{}", id);
        salaryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<SalaryAdjustmentDTO> addAdjustment(
            @PathVariable Long id,
            @RequestBody SalaryAdjustmentRequest request) {
        log.info("Endpoint: POST /salary/{}/adjustments type={}", id, request.getType());
        return ResponseEntity.ok(salaryService.addAdjustment(id, request));
    }

    @DeleteMapping("/{id}/adjustments/{adjId}")
    public ResponseEntity<Void> removeAdjustment(
            @PathVariable Long id,
            @PathVariable Long adjId) {
        log.info("Endpoint: DELETE /salary/{}/adjustments/{}", id, adjId);
        salaryService.removeAdjustment(id, adjId);
        return ResponseEntity.noContent().build();
    }
}

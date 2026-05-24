package com.tappy.pos.controller.employee;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.employee.CommissionReportDTO;
import com.tappy.pos.model.dto.employee.MyCommissionDTO;
import com.tappy.pos.service.employee.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/commission")
@RequiredArgsConstructor
@RequiresFeature("COMMISSION")
public class CommissionController {

    private final CommissionService commissionService;

    /**
     * GET /commission/me?month=5&year=2026
     * Returns the authenticated user's commission summary + item list for the given month/year.
     * Defaults to the current month/year if not supplied.
     */
    @GetMapping("/me")
    public ResponseEntity<MyCommissionDTO> getMyCommission(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication auth) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year  != null) ? year  : now.getYear();
        log.info("Endpoint: GET /commission/me month={} year={} user={}", m, y, auth.getName());
        return ResponseEntity.ok(commissionService.getMyCommission(auth.getName(), m, y));
    }

    /**
     * GET /commission/report?month=5&year=2026
     * Returns the team-wide commission report.
     * Requires COMMISSION_VIEW_ALL sub-feature.
     */
    @GetMapping("/report")
    @RequiresFeature("COMMISSION_VIEW_ALL")
    public ResponseEntity<CommissionReportDTO> getReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year  != null) ? year  : now.getYear();
        log.info("Endpoint: GET /commission/report month={} year={}", m, y);
        return ResponseEntity.ok(commissionService.getCommissionReport(m, y));
    }
}

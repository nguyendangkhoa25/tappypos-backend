package com.tappy.pos.controller.dashboard;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.dashboard.DashboardKpiDTO;
import com.tappy.pos.model.dto.dashboard.DashboardSummaryDTO;
import com.tappy.pos.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@RequiresFeature("DASHBOARD")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        log.info("Endpoint: GET /dashboard/summary");
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/kpi")
    public ResponseEntity<DashboardKpiDTO> getKpi(
            @RequestParam String from,
            @RequestParam String to) {
        log.info("Endpoint: GET /dashboard/kpi from={} to={}", from, to);
        return ResponseEntity.ok(dashboardService.getKpi(
                LocalDate.parse(from).atStartOfDay(),
                LocalDate.parse(to).atTime(LocalTime.MAX),
                from,
                to));
    }
}
